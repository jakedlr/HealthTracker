package com.jakedlr.healthtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jakedlr.healthtracker.domain.SleepSpan

/** One sleep period. The day it counts toward is derived with sleepDate(), never stored. */
@Entity(tableName = "sleep_entry", indices = [Index("start_at")])
data class SleepEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_at") val startAt: Long,
    @ColumnInfo(name = "end_at") val endAt: Long,
) {
    fun toSpan() = SleepSpan(startAt, endAt)
}
