package com.jakedlr.healthtracker

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras

class HealthTrackerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Shortcut for ViewModel factories: `initializer { FooViewModel(appContainer().fooRepository) }`. */
fun CreationExtras.appContainer(): AppContainer =
    (this[APPLICATION_KEY] as HealthTrackerApplication).container
