package com.alpefe.fujiptp.ui

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alpefe.fujiptp.FujiUsbManager
import com.alpefe.fujiptp.UsbIo
import com.alpefe.fujiptp.data.AppDatabase
import com.alpefe.fujiptp.data.CameraClient
import com.alpefe.fujiptp.data.RecipeModel
import com.alpefe.fujiptp.data.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One of the 7 camera slots with its currently assigned recipe. */
data class SlotUi(
    val index: Int,
    val recipe: RecipeModel?,
    /** True when the recipe was read from the camera and not yet saved. */
    val fromCamera: Boolean = false,
)

/** A collection shown in the library. */
data class CollectionUi(
    val id: Long,
    val name: String,
    val colorHex: Long,
    val isDefault: Boolean,
    val count: Int,
)

/** Screens of the app. */
sealed interface Screen {
    data object Active : Screen
    data object Backlog : Screen
    data object Discover : Screen
    data class Collection(val collectionId: Long, val name: String) : Screen
    data class DiscoverCollection(val id: String, val name: String) : Screen
    data class DiscoverRecipeDetail(val collectionId: String, val recipeId: String) : Screen
    data class Editor(
        val recipeId: Long?,
        val fromSlot: Int?,
        val assignOnSave: Int? = null,
        val collectionId: Long? = null,
    ) : Screen
}

class FujiViewModel(app: Application) : AndroidViewModel(app) {

    private val usbManager = FujiUsbManager(app)
    private val repo = RecipeRepository(AppDatabase.get(app).recipeDao())

    // --- persisted data ----------------------------------------------------
    val backlog: StateFlow<List<RecipeModel>> = repo.backlog
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Recipes read from the camera, held in memory (not in the library). */
    private val cameraRecipesInternal = MutableStateFlow<List<RecipeModel>?>(null)

    /**
     * User overrides for individual slots while the camera is connected:
     * slot index -> recipe. These are what make the profile diverge from
     * the camera before the user sends the profile back.
     */
    private val pendingOverrides = MutableStateFlow<Map<Int, RecipeModel>>(emptyMap())

    /** Public view of the camera's loaded recipes (read-only, only when connected). */
    val cameraRecipes: StateFlow<List<RecipeModel>> = cameraRecipesInternal
        .map { it ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** True when the camera is connected and its recipes have been read. */
    val hasCameraRecipes: StateFlow<Boolean> = cameraRecipesInternal
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * The 7 slots are the camera profile:
     * - No camera loaded -> all empty (profile is not modifiable).
     * - Camera loaded -> the camera's recipes, with any pending user
     *   overrides applied on top (assignments made from the library).
     */
    val slots: StateFlow<List<SlotUi>> =
        combine(cameraRecipesInternal, pendingOverrides) { cam, overrides ->
            if (cam == null) {
                (1..7).map { SlotUi(it, null) }
            } else {
                (1..7).map { i ->
                    val override = overrides[i]
                    SlotUi(i, override ?: cam[i - 1])
                }
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), (1..7).map { SlotUi(it, null) })

    val collections: StateFlow<List<CollectionUi>> = repo.collections
        .map { list -> list.map { CollectionUi(it.id, it.name, it.colorHex, it.isDefault, it.count) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Currently selected collection in the library. */
    private val selectedCollectionId = MutableStateFlow<Long?>(null)

    /** Recipes visible in the library for the currently selected collection. */
    val libraryRecipes: StateFlow<List<RecipeModel>> =
        combine(repo.collections, selectedCollectionId) { collections, selected ->
            val collection = collections.firstOrNull { it.id == selected }
                ?: collections.firstOrNull()
            collection?.id
        }
            .flatMapLatest { collectionId ->
                if (collectionId == null) flowOf(emptyList())
                else if (collections.value.firstOrNull { it.id == collectionId }?.isDefault == true) repo.backlog
                else repo.recipesInCollection(collectionId)
            }
            .map { list -> list.map { it.toModel() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Recipes of a specific collection (used inside CollectionScreen). */
    fun recipesInCollectionFlow(collectionId: Long): Flow<List<RecipeModel>> =
        repo.recipesInCollection(collectionId)
            .map { list -> list.map { it.toModel() } }

    // --- transient UI state ------------------------------------------------
    val connected = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val devicePresent = MutableStateFlow(false)
    val cameraLabel = MutableStateFlow<String?>(null)
    val messages = Channel<String>(Channel.BUFFERED)

    /** One-shot USB permission request consumed by MainActivity. */
    val permissionRequest = MutableStateFlow<UsbDevice?>(null)

    // --- navigation --------------------------------------------------------
    private val backstack = MutableStateFlow<List<Screen>>(listOf(Screen.Active))
    val screen: StateFlow<Screen> = backstack.map { it.last() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Screen.Active)

    private var client: CameraClient? = null
    private var bridge: UsbIo? = null

    fun push(screen: Screen) {
        backstack.value = backstack.value + screen
    }

    fun pop() {
        if (backstack.value.size > 1) {
            backstack.value = backstack.value.dropLast(1)
        }
    }

    fun notifyUser(message: String) {
        messages.trySend(message)
    }

    private suspend fun <T> withBusy(block: suspend () -> T): T {
        busy.value = true
        return try {
            block()
        } finally {
            busy.value = false
        }
    }

    // --- USB ---------------------------------------------------------------

    fun refreshDevicePresence() {
        val device = usbManager.findPtpCamera()
        devicePresent.value = device != null
        cameraLabel.value = device?.let { it.productName ?: it.deviceName }
        // Auto-connect when permission was already granted (e.g. app relaunch).
        if (device != null && usbManager.hasPermission(device) && !connected.value) {
            connectWithBridge(device)
        }
    }

    /** Called by MainActivity when the user asks to connect (may prompt). */
    fun connectRequested() {
        val device = usbManager.findPtpCamera()
        if (device == null) {
            notifyUser("No se detectó ninguna cámara Fujifilm. Conéctala por USB en modo RAW CONV./BACKUP RESTORE.")
            return
        }
        if (usbManager.hasPermission(device)) {
            connectWithBridge(device)
        } else {
            permissionRequest.value = device
        }
    }

    private fun connectWithBridge(device: UsbDevice) {
        viewModelScope.launch {
            withBusy {
                try {
                    val io = withContext(Dispatchers.IO) { usbManager.openBridge(device) }
                    val camera = CameraClient(io)
                    withContext(Dispatchers.IO) { camera.connect() }
                    withContext(Dispatchers.IO) { camera.openSession() }
                    client = camera
                    bridge = io
                    connected.value = true
                    notifyUser("Cámara conectada")
                    // La X100VI tarda un instante en estar lista tras abrir
                    // la sesión; esperamos antes de leer propiedades.
                    kotlinx.coroutines.delay(600)
                } catch (e: Exception) {
                    notifyUser("Error de conexión: ${e.message ?: "desconocido"}")
                    connected.value = false
                }
            }
            // Tras conectar, lee los 7 slots de la cámara automáticamente.
            readFromCamera()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            withBusy {
                val camera = client
                val io = bridge
                withContext(Dispatchers.IO) {
                    // Best effort, in order: close PTP session, then drop the
                    // controller, then release the USB connection. Each step
                    // is individually guarded so one failure can't leak the
                    // others (this is what previously required a full app
                    // restart).
                    runCatching { camera?.closeSession() }
                    runCatching { camera?.close() }
                    runCatching { io?.close() }
                }
                client = null
                bridge = null
                connected.value = false
                cameraRecipesInternal.value = null
                pendingOverrides.value = emptyMap()
                notifyUser("Desconectado")
            }
        }
    }

    /** Hard reset of the native session state (used after a failed op). */
    fun resetNativeSession() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { client?.close() }
                runCatching { bridge?.close() }
            }
            client = null
            bridge = null
            connected.value = false
            cameraRecipesInternal.value = null
            pendingOverrides.value = emptyMap()
        }
    }

    fun onBridgeReady(io: UsbIo) {
        viewModelScope.launch {
            withBusy {
                try {
                    val camera = CameraClient(io)
                    withContext(Dispatchers.IO) { camera.connect() }
                    withContext(Dispatchers.IO) { camera.openSession() }
                    client = camera
                    bridge = io
                    connected.value = true
                    notifyUser("Cámara conectada")
                    // La X100VI tarda un instante en estar lista tras abrir
                    // la sesión; esperamos antes de leer propiedades.
                    kotlinx.coroutines.delay(600)
                } catch (e: Exception) {
                    notifyUser("Error de conexión: ${e.message ?: "desconocido"}")
                    runCatching { io.close() }
                    connected.value = false
                }
            }
            // Tras conectar, lee los 7 slots de la cámara automáticamente.
            readFromCamera()
        }
    }

    fun permissionHandled() {
        permissionRequest.value = null
    }

    // --- camera operations ---------------------------------------------------

    fun readFromCamera() {
        viewModelScope.launch {
            val camera = client
            if (camera == null) {
                notifyUser("Conecta la cámara primero")
                return@launch
            }
            withBusy {
                try {
                    val recipes = withContext(Dispatchers.IO) { camera.readRecipes() }.getOrThrow()
                    // Kept in memory only: shown in the profile. Loading from
                    // the camera resets any pending overrides (the camera is
                    // the source of truth again).
                    cameraRecipesInternal.value = recipes
                    pendingOverrides.value = emptyMap()
                    notifyUser("C1–C7 leídos de la cámara")
                } catch (e: Exception) {
                    notifyUser("Error leyendo recipes: ${e.message ?: "desconocido"}")
                    // The PTP session may be in a bad state; reset it so the
                    // next operation (reconnect) starts clean.
                    resetNativeSession()
                }
            }
        }
    }

    /** Saves a camera recipe into the library (from the "En la cámara" view). */
    fun saveCameraRecipe(slot: Int, recipe: RecipeModel) {
        viewModelScope.launch {
            withBusy {
                try {
                    withContext(Dispatchers.IO) { repo.saveSlotRecipe(recipe, slot) }
                    notifyUser("«${recipe.name}» guardada en la biblioteca")
                } catch (e: Exception) {
                    notifyUser("Error al guardar: ${e.message ?: "desconocido"}")
                }
            }
        }
    }

    /** Transfers ALL the user's active slot recipes to the camera. */
    fun sendAllToCamera() {
        viewModelScope.launch {
            val camera = client
            if (camera == null) {
                notifyUser("Conecta la cámara primero")
                return@launch
            }
            val current = slots.value
            withBusy {
                var ok = 0
                var failed = 0
                for (slot in 1..7) {
                    val recipe = current[slot - 1].recipe
                    try {
                        // Empty recipe = clear that slot on the camera.
                        withContext(Dispatchers.IO) {
                            camera.writeRecipe(slot, recipe ?: RecipeModel(name = ""))
                        }
                        ok++
                    } catch (e: Exception) {
                        failed++
                    }
                }
                notifyUser(
                    if (failed == 0) "Se enviaron $ok recipes a la cámara"
                    else "$ok enviadas, $failed fallaron"
                )
                // The camera is now the source of truth: refresh and drop
                // all pending overrides.
                pendingOverrides.value = emptyMap()
                readFromCamera()
            }
        }
    }

    fun sendToSlot(slot: Int, recipe: RecipeModel) {
        viewModelScope.launch {
            val camera = client
            if (camera == null) {
                notifyUser("Conecta la cámara primero")
                return@launch
            }
            withBusy {
                try {
                    withContext(Dispatchers.IO) { camera.writeRecipe(slot, recipe) }
                    // The camera now has it: drop any pending override for
                    // that slot and refresh the profile.
                    pendingOverrides.value = pendingOverrides.value - slot
                    notifyUser("Recipe enviada a C$slot")
                    readFromCamera()
                } catch (e: Exception) {
                    notifyUser("Error escribiendo C$slot: ${e.message ?: "desconocido"}")
                }
            }
        }
    }

    /** Sends a backlog recipe to a camera slot (recipe must be saved). */
    fun sendRecipeToSlot(recipeId: Long, slot: Int) {
        viewModelScope.launch {
            val camera = client
            if (camera == null) {
                notifyUser("Conecta la cámara primero")
                return@launch
            }
            val recipe = repo.get(recipeId)
            if (recipe == null) {
                notifyUser("Recipe no encontrada")
                return@launch
            }
            withBusy {
                try {
                    withContext(Dispatchers.IO) { camera.writeRecipe(slot, recipe) }
                    pendingOverrides.value = pendingOverrides.value - slot
                    notifyUser("Recipe enviada a C$slot")
                } catch (e: Exception) {
                    notifyUser("Error escribiendo C$slot: ${e.message ?: "desconocido"}")
                }
            }
        }
    }

    /** Loads a saved recipe for editing (suspend; call from composition). */
    suspend fun getRecipe(id: Long): RecipeModel? = repo.get(id)

    // --- backlog CRUD ---------------------------------------------------------

    fun saveRecipe(recipe: RecipeModel, assignOnSave: Int?, collectionId: Long?) {
        viewModelScope.launch {
            withBusy {
                val id = withContext(Dispatchers.IO) { repo.save(recipe, collectionId) }
                if (assignOnSave != null) {
                    withContext(Dispatchers.IO) { repo.assignToSlot(assignOnSave, id) }
                }
                notifyUser("Recipe guardada")
                pop()
            }
        }
    }

    fun deleteRecipe(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.delete(id) }
            notifyUser("Recipe eliminada")
        }
    }

    fun deleteRecipes(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { ids.forEach { repo.delete(it) } }
            notifyUser("${ids.size} recipe${if (ids.size == 1) "" else "s"} eliminada${if (ids.size == 1) "" else "s"}")
        }
    }

    fun duplicateRecipe(id: Long) {
        viewModelScope.launch {
            val newId = withContext(Dispatchers.IO) { repo.duplicate(id) }
            if (newId > 0) {
                notifyUser("Recipe duplicada")
                push(Screen.Editor(newId, null))
            }
        }
    }

    /**
     * Assigns a library recipe to a profile slot WITHOUT sending it to the
     * camera. Only possible while the camera is connected (the profile is a
     * mirror of the camera). The assignment becomes a pending override shown
     * in the profile; it is written to the camera on the next send.
     */
    fun assignToSlot(slot: Int, recipeId: Long) {
        if (cameraRecipesInternal.value == null) {
            notifyUser("Conecta la cámara para poder asignar a un slot")
            return
        }
        val recipe = backlog.value.firstOrNull { it.id == recipeId }
        if (recipe == null) {
            notifyUser("Recipe no encontrada")
            return
        }
        pendingOverrides.value = pendingOverrides.value + (slot to recipe)
        notifyUser("«${recipe.name}» asignada a C$slot (pendiente de enviar)")
    }

    /** Removes a pending override for a slot (falls back to the camera value). */
    fun clearSlot(slot: Int) {
        val cam = cameraRecipesInternal.value ?: run {
            notifyUser("Conecta la cámara para modificar el perfil")
            return
        }
        val overrides = pendingOverrides.value
        pendingOverrides.value = overrides - slot
        // If there was no override, this clears the slot entirely (will be
        // sent as empty when the profile is pushed to the camera).
        if (slot !in overrides) {
            pendingOverrides.value = pendingOverrides.value + (slot to RecipeModel(name = ""))
        }
        notifyUser("C$slot vaciado")
    }

    // --- collections -----------------------------------------------------------

    /** Selects a collection in the library (null = default selection). */
    fun selectCollection(id: Long) {
        selectedCollectionId.value = id
    }

    fun createCollection(name: String, colorHex: Long) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { repo.createCollection(name, colorHex) }
            if (id > 0) {
                selectedCollectionId.value = id
                notifyUser("Colección «$name» creada")
            }
        }
    }

    fun renameCollection(id: Long, name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.renameCollection(id, name) }
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) { repo.deleteCollection(id) }
            if (deleted) {
                if (selectedCollectionId.value == id) selectedCollectionId.value = null
                notifyUser("Colección eliminada")
            } else {
                notifyUser("La colección por defecto no se puede eliminar")
            }
        }
    }

    fun addToCollection(recipeId: Long, collectionId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.addRecipeToCollection(recipeId, collectionId) }
        }
    }

    fun removeFromCollection(recipeId: Long, collectionId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.removeRecipeFromCollection(recipeId, collectionId) }
        }
    }

    /** Imports a Discover recipe into one of the user's collections. */
    fun importDiscoverRecipe(name: String, filmSimulation: String, collectionId: Long) {
        importDiscoverRecipes(listOf(name to filmSimulation), collectionId)
    }

    /** Imports multiple Discover recipes (or a whole collection) at once. */
    fun importDiscoverRecipes(recipes: List<Pair<String, String>>, collectionId: Long) {
        if (recipes.isEmpty()) return
        viewModelScope.launch {
            var count = 0
            for ((name, filmSimulation) in recipes) {
                val discover = com.alpefe.fujiptp.data.DiscoverData.collections
                    .flatMap { it.recipes }
                    .firstOrNull { it.name == name && it.filmSimulation == filmSimulation }
                val recipe = discover?.toModel()
                    ?: RecipeModel(
                        name = name,
                        filmSimulation = com.alpefe.fujiptp.data.FilmSimulation.entries
                            .firstOrNull { it.name == filmSimulation }
                            ?: com.alpefe.fujiptp.data.FilmSimulation.ClassicChrome,
                    )
                val id = withContext(Dispatchers.IO) { repo.save(recipe, collectionId) }
                if (id > 0) count++
            }
            notifyUser(
                if (count == recipes.size) "$count recipes importadas a tu colección"
                else "$count de ${recipes.size} recipes importadas"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        val camera = client
        val io = bridge
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { camera?.closeSession() }
                runCatching { camera?.close() }
                io?.close()
            }
        }
    }
}
