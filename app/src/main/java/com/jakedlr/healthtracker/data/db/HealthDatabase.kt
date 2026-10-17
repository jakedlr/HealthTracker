package com.jakedlr.healthtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Every schema change after v1 must ship a Migration; never use destructive fallback.
@Database(
    entities = [WeightEntry::class, FoodItem::class, FoodLog::class, SleepEntry::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao
    abstract fun foodDao(): FoodDao
    abstract fun sleepDao(): SleepDao

    companion object {
        const val NAME = "health.db"

        fun build(context: Context): HealthDatabase =
            Room.databaseBuilder(context, HealthDatabase::class.java, NAME)
                .addMigrations(*ALL_MIGRATIONS)
                .build()
    }
}
