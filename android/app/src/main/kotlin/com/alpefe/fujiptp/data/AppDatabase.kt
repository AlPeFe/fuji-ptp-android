package com.alpefe.fujiptp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RecipeEntity::class, SlotEntity::class, CollectionEntity::class, RecipeCollectionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * v1 -> v2: collections + recipe_collections tables, plus the default
         * "Todas" collection seeded with every existing recipe.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `collections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `recipe_collections` (
                        `recipeId` INTEGER NOT NULL,
                        `collectionId` INTEGER NOT NULL,
                        PRIMARY KEY(`recipeId`, `collectionId`),
                        FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_collections_collectionId` ON `recipe_collections` (`collectionId`)")
                val now = System.currentTimeMillis()
                db.execSQL(
                    """INSERT INTO `collections` (`name`, `colorHex`, `isDefault`, `createdAt`, `updatedAt`)
                       VALUES ('Todas', 4289624258, 1, $now, $now)"""
                )
                db.execSQL(
                    """INSERT INTO `recipe_collections` (`recipeId`, `collectionId`)
                       SELECT r.id, c.id FROM `recipes` r, `collections` c WHERE c.isDefault = 1"""
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fuji_recipes.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
