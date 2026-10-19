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

/** v3: sleep entries. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_entry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`start_at` INTEGER NOT NULL, `end_at` INTEGER NOT NULL)",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_sleep_entry_start_at` ON `sleep_entry` (`start_at`)")
    }
}

/** v4: weight training (exercise categories, exercises, exercise logs). */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercise_category` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL COLLATE NOCASE, `position` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_exercise_category_name` ON `exercise_category` (`name`)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercise` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`category_id` INTEGER NOT NULL, `name` TEXT NOT NULL COLLATE NOCASE, `sidedness` TEXT NOT NULL, " +
                "`default_unit` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`category_id`) REFERENCES `exercise_category`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_exercise_name` ON `exercise` (`name`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_category_id` ON `exercise` (`category_id`)")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercise_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`exercise_id` INTEGER NOT NULL, `date` INTEGER NOT NULL, `unit` TEXT NOT NULL, `load` REAL, " +
                "`reps` INTEGER, `left_load` REAL, `left_reps` INTEGER, `right_load` REAL, `right_reps` INTEGER, " +
                "FOREIGN KEY(`exercise_id`) REFERENCES `exercise`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_exercise_log_exercise_id_date` " +
                "ON `exercise_log` (`exercise_id`, `date`)",
        )
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
