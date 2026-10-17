package com.jakedlr.healthtracker.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.data.db.FoodLog
import com.jakedlr.healthtracker.data.db.SleepEntry
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.ConfirmDeleteDialog
import com.jakedlr.healthtracker.ui.formatAmount
import com.jakedlr.healthtracker.ui.formatDuration
import com.jakedlr.healthtracker.ui.formatGrams
import com.jakedlr.healthtracker.ui.formatKcal
import com.jakedlr.healthtracker.ui.formatLbs
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    onBack: () -> Unit,
    onEditWeight: (LocalDate) -> Unit,
    onAddFood: (LocalDate) -> Unit,
    onEditFoodLog: (logId: Long) -> Unit,
    onAddSleep: (LocalDate) -> Unit,
    onEditSleep: (sleepId: Long) -> Unit,
    viewModel: DayViewModel = viewModel(factory = DayViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val title = state.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))

    Scaffold(
        topBar = { TopAppBar(title = { Text(title, maxLines = 1) }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        if (state.loading) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Weight
            Section("Weight") {
                val weight = state.weight
                if (weight == null) {
                    Text("No weigh-in", style = MaterialTheme.typography.bodyLarge)
                    SectionButton("Add weight") { onEditWeight(state.date) }
                } else {
                    Text("${formatLbs(weight.weightLbs)} lbs", style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEditWeight(state.date) }) { Text("Edit") }
                        OutlinedButton(onClick = viewModel::requestWeightDelete) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Food
            Section("Food") {
                val n = state.nutrition
                if (n == null) {
                    Text("Nothing logged", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("${formatKcal(n.caloriesKcal)} kcal", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Protein ${formatGrams(n.proteinG)} g · Carbs ${formatGrams(n.carbsG)} g\n" +
                            "Fat ${formatGrams(n.fatG)} g · Fiber ${formatGrams(n.fiberG)} g",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    state.foodLogs.forEach { log ->
                        HorizontalDivider()
                        FoodLogRow(log, state.zone) { onEditFoodLog(log.id) }
                    }
                }
                SectionButton("Add food") { onAddFood(state.date) }
            }

            // Sleep
            Section("Sleep") {
                val total = state.sleepMillis
                if (total == null) {
                    Text("No sleep counted toward this day", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text(formatDuration(total), style = MaterialTheme.typography.headlineSmall)
                    state.sleep.forEach { entry ->
                        HorizontalDivider()
                        SleepRow(entry, state.date, state.zone) { onEditSleep(entry.id) }
                    }
                }
                SectionButton("Add sleep") { onAddSleep(state.date) }
            }
        }
    }

    if (state.confirmingWeightDelete) {
        ConfirmDeleteDialog(
            title = "Delete this weigh-in?",
            text = "This can't be undone.",
            onConfirm = viewModel::confirmWeightDelete,
            onDismiss = viewModel::cancelWeightDelete,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            content()
        }
    }
}

@Composable
private fun SectionButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(label) }
}

private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

@Composable
private fun FoodLogRow(log: FoodLog, zone: ZoneId, onClick: () -> Unit) {
    val time = Instant.ofEpochMilli(log.eatenAt).atZone(zone).toLocalTime().format(timeFormat)
    ListItem(
        headlineContent = { Text(log.name) },
        supportingContent = { Text("$time · ${formatAmount(log.servings)} serving${if (log.servings == 1.0) "" else "s"}") },
        trailingContent = { Text("${formatKcal(log.toLoggedFood().total.caloriesKcal)} kcal") },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Shows times; adds the date for a side that isn't on the day itself (e.g. waking the next morning). */
@Composable
private fun SleepRow(entry: SleepEntry, day: LocalDate, zone: ZoneId, onClick: () -> Unit) {
    val dateFormat = DateTimeFormatter.ofPattern("MMM d")
    fun label(millis: Long): String {
        val t = Instant.ofEpochMilli(millis).atZone(zone)
        val time = t.toLocalTime().format(timeFormat)
        return if (t.toLocalDate() == day) time else "${t.toLocalDate().format(dateFormat)}, $time"
    }
    ListItem(
        headlineContent = { Text("${label(entry.startAt)} – ${label(entry.endAt)}") },
        trailingContent = { Text(formatDuration(entry.endAt - entry.startAt)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
