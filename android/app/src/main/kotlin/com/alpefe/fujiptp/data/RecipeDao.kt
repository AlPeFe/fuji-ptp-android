package com.alpefe.fujiptp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): RecipeEntity?

    @Query("SELECT * FROM recipes")
    suspend fun getAll(): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: RecipeEntity): Long

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    // --- slots ------------------------------------------------------------

    @Query(
        """SELECT s.slotIndex AS slotIndex, r.* FROM slots s
           LEFT JOIN recipes r ON s.recipeId = r.id
           ORDER BY s.slotIndex"""
    )
    fun observeSlots(): Flow<List<SlotWithRecipe>>

    @Query("SELECT * FROM slots WHERE slotIndex = :slot")
    suspend fun getSlot(slot: Int): SlotEntity?

    @Query("SELECT * FROM slots")
    suspend fun getAllSlots(): List<SlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlot(slot: SlotEntity)

    @Query("SELECT slotIndex, recipeId FROM slots WHERE recipeId = :recipeId")
    suspend fun slotForRecipe(recipeId: Long): List<SlotIndexRow>

    @Query("DELETE FROM slots WHERE recipeId = :recipeId")
    suspend fun unassignRecipe(recipeId: Long)

    data class SlotIndexRow(val slotIndex: Int, val recipeId: Long?)
}
