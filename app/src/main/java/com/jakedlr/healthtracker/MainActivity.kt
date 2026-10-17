package com.jakedlr.healthtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jakedlr.healthtracker.ui.navigation.HealthTrackerNavHost
import com.jakedlr.healthtracker.ui.theme.HealthTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthTrackerTheme {
                // Paint the theme background behind every screen so screen transitions fade over
                // it rather than the window (which would flash light in dark mode).
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HealthTrackerNavHost()
                }
            }
        }
    }
}
