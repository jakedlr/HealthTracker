package com.jakedlr.healthtracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.ui.formatLbs
import com.jakedlr.healthtracker.ui.theme.HealthTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(
    onAddWeight: () -> Unit,
    onOpenGraphs: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    HomeContent(state, onAddWeight, onOpenGraphs)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onAddWeight: () -> Unit,
    onOpenGraphs: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Health Tracker") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                state.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = MaterialTheme.typography.titleMedium,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Weight", style = MaterialTheme.typography.labelLarge)
                    Text(
                        state.todayWeightLbs?.let { "${formatLbs(it)} lbs" } ?: "No weigh-in today",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
            Button(onClick = onAddWeight, modifier = Modifier.fillMaxWidth()) { Text("Add weight") }
            OutlinedButton(onClick = onOpenGraphs, modifier = Modifier.fillMaxWidth()) { Text("Graphs") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    HealthTrackerTheme {
        HomeContent(HomeUiState(LocalDate.of(2026, 10, 16), 182.4), {}, {})
    }
}
