package com.jakedlr.healthtracker.data

import android.content.Context
import com.jakedlr.healthtracker.domain.GraphRange

/** Remembers the graph range between launches (on-device SharedPreferences). */
class GraphPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("graphs", Context.MODE_PRIVATE)

    var range: GraphRange
        get() = prefs.getString(KEY_RANGE, null)
            ?.let { name -> GraphRange.entries.firstOrNull { it.name == name } }
            ?: GraphRange.MONTH
        set(value) = prefs.edit().putString(KEY_RANGE, value.name).apply()

    private companion object {
        const val KEY_RANGE = "range"
    }
}
