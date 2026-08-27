package com.alpefe.fujiptp.data

import kotlinx.coroutines.flow.Flow

/** Single source of truth for recipes, collections and the 7 active slots. */
class RecipeRepository(private val dao: RecipeDao) {

    val backlog: Flow<List<RecipeEntity>> = dao.observeAll()
    val slots: Flow<List<SlotWithRecipe>> = dao.observeSlots()
    val collections: Flow<List<CollectionWithCount>> = dao.observeCollections()

    fun recipesInCollection(collectionId: Long): Flow<List<RecipeEntity>> =
        dao.observeRecipesInCollection(collectionId)

    // --- recipes ------------------------------------------------------------

    suspend fun save(recipe: RecipeModel, collectionId: Long? = null): Long {
        val now = System.currentTimeMillis()
        val id = dao.upsert(RecipeEntity.fromModel(recipe, now))
        // New recipes land in the given collection, or the default one.
        val target = collectionId ?: dao.getDefaultCollectionId()
        if (target != null) {
            dao.addRecipeToCollection(id, target)
        }
        return id
    }

    suspend fun get(id: Long): RecipeModel? = dao.getById(id)?.toModel()

    /** Returns the first recipe with this exact name (used for dedupe). */
    suspend fun findByName(name: String): RecipeEntity? = dao.findByName(name)

    /** Updates an existing recipe's values, keeping its id. */
    suspend fun updateRecipe(recipe: RecipeModel) {
        dao.upsert(RecipeEntity.fromModel(recipe, System.currentTimeMillis()))
    }

    suspend fun delete(id: Long) {
        dao.unassignRecipe(id)
        dao.delete(id)
    }

    suspend fun duplicate(id: Long): Long {
        val source = dao.getById(id) ?: return -1L
        val copy = source.copy(
            id = 0L,
            name = source.name + " copy",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val newId = dao.upsert(copy)
        // The duplicate inherits its source collections.
        dao.collectionIdsForRecipe(id).forEach { dao.addRecipeToCollection(newId, it) }
        return newId
    }

    // --- slots ----------------------------------------------------------------

    suspend fun assignToSlot(slot: Int, recipeId: Long) {
        dao.upsertSlot(SlotEntity(slot, recipeId, System.currentTimeMillis()))
    }

    suspend fun clearSlot(slot: Int) {
        dao.upsertSlot(SlotEntity(slot, null, System.currentTimeMillis()))
    }

    /**
     * Applies the 7 recipes read from the camera to the slot assignments
     * WITHOUT persisting them to the library. The caller keeps the camera
     * recipes in memory and shows them on the slots; only an explicit
     * "save" action stores a recipe into the library (and the default
     * collection).
     */
    suspend fun applyCameraRecipes(recipes: List<RecipeModel>) {
        require(recipes.size == 7) { "camera must return 7 recipes" }
        // Slots are left untouched here: the UI shows the in-memory camera
        // recipes. Persisting happens only on explicit save.
    }

    /**
     * Explicitly saves a camera recipe into the library (default collection)
     * and remembers the slot assignment.
     */
    suspend fun saveSlotRecipe(recipe: RecipeModel, slot: Int): Long {
        val id = dao.upsert(RecipeEntity.fromModel(recipe, System.currentTimeMillis()))
        dao.getDefaultCollectionId()?.let { dao.addRecipeToCollection(id, it) }
        dao.upsertSlot(SlotEntity(slot, id, System.currentTimeMillis()))
        return id
    }

    // --- collections ----------------------------------------------------------

    /**
     * Creates a collection. Returns its id.
     */
    suspend fun createCollection(name: String, colorHex: Long): Long {
        val now = System.currentTimeMillis()
        return dao.upsertCollection(
            CollectionEntity(
                name = name.ifBlank { "Nueva colección" },
                colorHex = colorHex,
                isDefault = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /**
     * Renames a collection. The default collection can also be renamed;
     * only deletion is forbidden for it.
     */
    suspend fun renameCollection(id: Long, name: String) {
        val collection = dao.getCollection(id) ?: return
        if (name.isNotBlank() && name != collection.name) {
            dao.updateCollection(collection.copy(name = name, updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Deletes a collection. The default collection cannot be deleted, but
     * any other collection can — including the last remaining one. If no
     * collections exist, the user simply creates one before adding recipes.
     */
    suspend fun deleteCollection(id: Long): Boolean {
        val collection = dao.getCollection(id) ?: return false
        if (collection.isDefault) return false
        dao.clearCollection(id)
        dao.deleteCollection(id)
        return true
    }

    /** Empties the whole library: every recipe, its slot assignments, and
     *  every non-default collection. The default collection survives. */
    suspend fun clearLibrary() {
        dao.clearAllMemberships()
        dao.clearAllRecipes()
        dao.clearAllSlots()
        dao.clearAllNonDefaultCollections()
    }

    suspend fun addRecipeToCollection(recipeId: Long, collectionId: Long) {
        dao.addRecipeToCollection(recipeId, collectionId)
    }

    suspend fun removeRecipeFromCollection(recipeId: Long, collectionId: Long) {
        val collection = dao.getCollection(collectionId)
        // The default collection cannot be emptied as a membership rule.
        if (collection?.isDefault == true) return
        dao.removeRecipeFromCollection(recipeId, collectionId)
    }

    suspend fun collectionIdsForRecipe(recipeId: Long): List<Long> =
        dao.collectionIdsForRecipe(recipeId)
}
