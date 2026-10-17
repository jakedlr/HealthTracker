package com.jakedlr.healthtracker

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.jakedlr.healthtracker.data.BackupRepository
import com.jakedlr.healthtracker.data.DatabaseHolder
import com.jakedlr.healthtracker.data.db.HealthDatabase
import java.time.ZoneId

class HealthTrackerApplication : Application(), DatabaseHolder {
    lateinit var container: AppContainer
        private set

    /** Lives outside [container] because an import replaces the container. */
    val backupRepository by lazy { BackupRepository(this, HealthDatabase.NAME, this, ZoneId.systemDefault()) }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override val database: HealthDatabase get() = container.database

    override fun closeDatabase() = container.database.close()

    /** Opens the database file afresh with a new container (new repositories, same app process). */
    override fun reopenDatabase() {
        container = AppContainer(this)
    }
}

/** Shortcut for ViewModel factories: `initializer { FooViewModel(appContainer().fooRepository) }`. */
fun CreationExtras.appContainer(): AppContainer = healthTrackerApp().container

fun CreationExtras.healthTrackerApp(): HealthTrackerApplication = this[APPLICATION_KEY] as HealthTrackerApplication
