package com.jakedlr.healthtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** One weigh-in per day; saving again for the same date overwrites it. */
@Entity(tableName = "weight_entry")
data class WeightEntry(
    @PrimaryKey val date: LocalDate,
    @ColumnInfo(name = "weight_lbs") val weightLbs: Double,
)
