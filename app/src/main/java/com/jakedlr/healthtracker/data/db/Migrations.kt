package com.jakedlr.healthtracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// SQL copied from the exported schema (app/schemas/.../2.json) so the result matches what Room expects.

/** v2: food library and food log. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL COLLATE NOCASE, `barcode` TEXT, `calories_kcal` REAL NOT NULL, " +
                "`protein_g` REAL NOT NULL, `carbs_g` REAL NOT NULL, `fiber_g` REAL NOT NULL, " +
                "`fat_g` REAL NOT NULL)",
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_item_name` ON `food_item` (`name`)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_item_barcode` ON `food_item` (`barcode`)")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`food_item_id` INTEGER, `eaten_at` INTEGER NOT NULL, `servings` REAL NOT NULL, " +
                "`name` TEXT NOT NULL, `calories_kcal` REAL NOT NULL, `protein_g` REAL NOT NULL, " +
                "`carbs_g` REAL NOT NULL, `fiber_g` REAL NOT NULL, `fat_g` REAL NOT NULL, " +
                "FOREIGN KEY(`food_item_id`) REFERENCES `food_item`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_food_log_food_item_id` ON `food_log` (`food_item_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_food_log_eaten_at` ON `food_log` (`eaten_at`)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
