package com.jakedlr.healthtracker

import android.content.Context
import com.jakedlr.healthtracker.data.FoodRepository
import com.jakedlr.healthtracker.data.GraphPreferences
import com.jakedlr.healthtracker.data.SleepRepository
import com.jakedlr.healthtracker.data.TrainingRepository
import com.jakedlr.healthtracker.data.WeightRepository
import com.jakedlr.healthtracker.data.db.HealthDatabase
import java.time.Clock

/**
 * Manual dependency wiring; one instance lives on [HealthTrackerApplication].
 * Replaced wholesale after an import, so nothing outside it should keep repository references.
 */
class AppContainer(context: Context) {
    val database = HealthDatabase.build(context)

    val clock: Clock = Clock.systemDefaultZone()
    val weightRepository = WeightRepository(database.weightDao())
    val foodRepository = FoodRepository(database, database.foodDao())
    val sleepRepository = SleepRepository(database, database.sleepDao())
    val trainingRepository = TrainingRepository(database, database.trainingDao())
    val graphPreferences = GraphPreferences(context)
}
