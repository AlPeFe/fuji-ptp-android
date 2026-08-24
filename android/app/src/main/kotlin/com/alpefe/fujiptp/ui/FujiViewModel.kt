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
data class SlotUi(val index: Int, val recipe: RecipeModel?)

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
    data class Editor(val recipeId: Long?, val fromSlot: Int?, val assignOnSave: Int? = null) : Screen
}

class FujiViewModel(app: Application) : AndroidViewModel(app) {

    private val usbManager = FujiUsbManager(app)
    private val repo = RecipeRepository(AppDatabase.get(app).recipeDao())

    // --- persisted data ----------------------------------------------------
    val backlog: StateFlow<List<RecipeModel>> = repo.backlog
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val slots: StateFlow<List<SlotUi>> = repo.slots
        .map { rows ->
            val byIndex = rows.associateBy { it.slotIndex }
            (1..7).map { i -> SlotUi(i, byIndex[i]?.recipe?.toModel()) }
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
                    runCatching { camera?.closeSession() }
                    runCatching { camera?.close() }
                    io?.close()
                }
                client = null
                bridge = null
                connected.value = false
                notifyUser("Desconectado")
            }
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
                    withContext(Dispatchers.IO) { repo.importFromCamera(recipes) }
                    notifyUser("C1–C7 importados de la cámara")
                } catch (e: Exception) {
                    notifyUser("Error leyendo recipes: ${e.message ?: "desconocido"}")
                }
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
                    // Unsaved recipes (id == 0) are persisted first so the
                    // slot assignment can reference them.
                    val id = if (recipe.id > 0) recipe.id
                    else withContext(Dispatchers.IO) { repo.save(recipe) }
                    val saved = repo.get(id)
                    if (saved == null) {
                        notifyUser("No se pudo guardar la recipe")
                        return@withBusy
                    }
                    withContext(Dispatchers.IO) { camera.writeRecipe(slot, saved) }
                    withContext(Dispatchers.IO) { repo.assignToSlot(slot, id) }
                    notifyUser("Recipe enviada a C$slot")
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
                    withContext(Dispatchers.IO) { repo.assignToSlot(slot, recipeId) }
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

    fun saveRecipe(recipe: RecipeModel, assignOnSave: Int?) {
        viewModelScope.launch {
            withBusy {
                val id = withContext(Dispatchers.IO) { repo.save(recipe) }
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

    fun duplicateRecipe(id: Long) {
        viewModelScope.launch {
            val newId = withContext(Dispatchers.IO) { repo.duplicate(id) }
            if (newId > 0) {
                notifyUser("Recipe duplicada")
                push(Screen.Editor(newId, null))
            }
        }
    }

    fun assignToSlot(slot: Int, recipeId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.assignToSlot(slot, recipeId) }
            notifyUser("Asignada a C$slot")
        }
    }

    fun clearSlot(slot: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.clearSlot(slot) }
            notifyUser("C$slot vaciado")
        }
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
                notifyUser("No se puede eliminar: debe quedar al menos una colección")
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
