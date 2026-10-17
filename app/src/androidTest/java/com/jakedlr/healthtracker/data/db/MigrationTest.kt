package com.jakedlr.healthtracker.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbFile = InstrumentationRegistry.getInstrumentation().targetContext
        .getDatabasePath("migration-test.db")

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = dbFile,
        driver = AndroidSQLiteDriver(),
        databaseClass = HealthDatabase::class,
    )

    @Test
    fun migrate1To2_keepsWeights_andAddsFoodTables() {
        helper.createDatabase(1).use { connection ->
            connection.execSQL("INSERT INTO weight_entry (date, weight_lbs) VALUES (20742, 182.4)")
        }

        // Validates the migrated schema against the exported 2.json.
        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).use { connection ->
            connection.prepare("SELECT date, weight_lbs FROM weight_entry").use { stmt ->
                stmt.step()
                assertEquals(20742L, stmt.getLong(0))
                assertEquals(182.4, stmt.getDouble(1), 0.0)
            }
            connection.execSQL(
                "INSERT INTO food_item (name, calories_kcal, protein_g, carbs_g, fiber_g, fat_g) " +
                    "VALUES ('Apple', 95, 0.5, 25, 4.4, 0.3)",
            )
            connection.execSQL(
                "INSERT INTO food_log (food_item_id, eaten_at, servings, name, calories_kcal, protein_g, " +
                    "carbs_g, fiber_g, fat_g) VALUES (1, 0, 1, 'Apple', 95, 0.5, 25, 4.4, 0.3)",
            )
            connection.prepare("SELECT COUNT(*) FROM food_log").use { stmt ->
                stmt.step()
                assertEquals(1L, stmt.getLong(0))
            }
        }
    }
}
