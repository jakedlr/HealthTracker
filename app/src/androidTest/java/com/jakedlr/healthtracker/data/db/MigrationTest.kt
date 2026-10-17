package com.jakedlr.healthtracker.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbFile = context.getDatabasePath(DB_NAME)

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = dbFile,
        driver = AndroidSQLiteDriver(),
        databaseClass = HealthDatabase::class,
    )

    // The driver-based helper doesn't clean up between tests, so start each one from scratch.
    @Before
    fun deleteDatabase() {
        context.deleteDatabase(DB_NAME)
    }

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

    @Test
    fun migrate2To3_keepsEverything_andAddsSleepTable() {
        helper.createDatabase(2).use { connection ->
            connection.execSQL("INSERT INTO weight_entry (date, weight_lbs) VALUES (20742, 182.4)")
            connection.execSQL(
                "INSERT INTO food_item (name, barcode, calories_kcal, protein_g, carbs_g, fiber_g, fat_g) " +
                    "VALUES ('Apple', '4006381333931', 95, 0.5, 25, 4.4, 0.3)",
            )
            connection.execSQL(
                "INSERT INTO food_log (food_item_id, eaten_at, servings, name, calories_kcal, protein_g, " +
                    "carbs_g, fiber_g, fat_g) VALUES (1, 0, 1, 'Apple', 95, 0.5, 25, 4.4, 0.3)",
            )
        }

        // Validates the migrated schema against the exported 3.json.
        helper.runMigrationsAndValidate(3, listOf(MIGRATION_2_3)).use { connection ->
            for ((table, expected) in listOf("weight_entry" to 1L, "food_item" to 1L, "food_log" to 1L)) {
                connection.prepare("SELECT COUNT(*) FROM $table").use { stmt ->
                    stmt.step()
                    assertEquals(table, expected, stmt.getLong(0))
                }
            }
            connection.prepare("SELECT barcode FROM food_item").use { stmt ->
                stmt.step()
                assertEquals("4006381333931", stmt.getText(0))
            }
            connection.execSQL("INSERT INTO sleep_entry (start_at, end_at) VALUES (1000, 2000)")
            connection.prepare("SELECT start_at, end_at FROM sleep_entry").use { stmt ->
                stmt.step()
                assertEquals(1000L, stmt.getLong(0))
                assertEquals(2000L, stmt.getLong(1))
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
