package com.jakedlr.healthtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** Current schema version. Imports accept files from version 1 up to this. */
const val HEALTH_DB_VERSION = 3

// Every schema change after v1 must ship a Migration; never use destructive fallback.
@Database(
    entities = [WeightEntry::class, FoodItem::class, FoodLog::class, SleepEntry::class],
    version = HEALTH_DB_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao
    abstract fun foodDao(): FoodDao
    abstract fun sleepDao(): SleepDao

    companion object {
        const val NAME = "health.db"

        fun build(context: Context, name: String = NAME): HealthDatabase =
            Room.databaseBuilder(context, HealthDatabase::class.java, name)
                .addMigrations(*ALL_MIGRATIONS)
                .build()
    }
}
