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

    suspend fun save(recipe: RecipeModel): Long {
        val now = System.currentTimeMillis()
        val id = dao.upsert(RecipeEntity.fromModel(recipe, now))
        // New recipes land in the default collection.
        if (recipe.id == 0L) {
            dao.getDefaultCollectionId()?.let { dao.addRecipeToCollection(id, it) }
        }
        return id
    }

    suspend fun get(id: Long): RecipeModel? = dao.getById(id)?.toModel()

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
     * Imports the 7 camera recipes into the backlog (and the default
     * collection), de-duplicating against existing recipes by value equality.
     */
    suspend fun importFromCamera(recipes: List<RecipeModel>): Map<Int, Long> {
        require(recipes.size == 7) { "camera must return 7 recipes" }
        val existing = dao.getAll().map { it.toModel() }
        val defaultCollectionId = dao.getDefaultCollectionId()
        val assignments = mutableMapOf<Int, Long>()
        for ((index, camera) in recipes.withIndex()) {
            val slot = index + 1
            val match = existing.firstOrNull { it.sameValuesAs(camera) }
            val id = if (match != null) {
                dao.upsert(RecipeEntity.fromModel(match, System.currentTimeMillis()))
                match.id
            } else {
                dao.upsert(RecipeEntity.fromModel(camera, System.currentTimeMillis()))
            }
            defaultCollectionId?.let { dao.addRecipeToCollection(id, it) }
            assignments[slot] = id
            dao.upsertSlot(SlotEntity(slot, id, System.currentTimeMillis()))
        }
        return assignments
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

    suspend fun renameCollection(id: Long, name: String) {
        val collection = dao.getCollection(id) ?: return
        if (name.isNotBlank() && name != collection.name) {
            dao.updateCollection(collection.copy(name = name, updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Deletes a collection (except the default "Todas"). The app always
     * keeps at least one collection: the last non-default one cannot be
     * removed.
     */
    suspend fun deleteCollection(id: Long): Boolean {
        val collection = dao.getCollection(id) ?: return false
        if (collection.isDefault) return false
        if (dao.collectionCount() <= 1) return false
        dao.clearCollection(id)
        dao.deleteCollection(id)
        return true
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
