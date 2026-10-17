package com.jakedlr.healthtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jakedlr.healthtracker.ui.navigation.HealthTrackerNavHost
import com.jakedlr.healthtracker.ui.theme.HealthTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthTrackerTheme {
                HealthTrackerNavHost()
            }
        }
    }
}
