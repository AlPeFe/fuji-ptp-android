package com.alpefe.fujiptp.data

import kotlinx.coroutines.flow.Flow

/** Single source of truth for recipes + the 7 active slots. */
class RecipeRepository(private val dao: RecipeDao) {

    val backlog: Flow<List<RecipeEntity>> = dao.observeAll()
    val slots: Flow<List<SlotWithRecipe>> = dao.observeSlots()

    suspend fun save(recipe: RecipeModel): Long {
        val now = System.currentTimeMillis()
        return dao.upsert(RecipeEntity.fromModel(recipe, now))
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
        return dao.upsert(copy)
    }

    suspend fun assignToSlot(slot: Int, recipeId: Long) {
        dao.upsertSlot(SlotEntity(slot, recipeId, System.currentTimeMillis()))
    }

    suspend fun clearSlot(slot: Int) {
        dao.upsertSlot(SlotEntity(slot, null, System.currentTimeMillis()))
    }

    suspend fun slotAssignments(): Map<Int, Long> =
        dao.getAllSlots().mapNotNull { slot -> slot.recipeId?.let { slot.slotIndex to it } }.toMap()

    /**
     * Imports the 7 camera recipes into the backlog, de-duplicating against
     * existing recipes by exact value equality, then updates the slots.
     * Returns the slot -> recipeId map.
     */
    suspend fun importFromCamera(recipes: List<RecipeModel>): Map<Int, Long> {
        require(recipes.size == 7) { "camera must return 7 recipes" }
        val existing = dao.getAll().map { it.toModel() }
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
            assignments[slot] = id
            dao.upsertSlot(SlotEntity(slot, id, System.currentTimeMillis()))
        }
        return assignments
    }
}
