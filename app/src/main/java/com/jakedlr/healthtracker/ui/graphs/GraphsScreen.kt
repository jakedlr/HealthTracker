package com.jakedlr.healthtracker.ui.graphs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.domain.GraphRange
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.formatDuration
import com.jakedlr.healthtracker.ui.formatGrams
import com.jakedlr.healthtracker.ui.formatKcal
import com.jakedlr.healthtracker.ui.formatLbs
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphsScreen(
    onBack: () -> Unit,
    viewModel: GraphsViewModel = viewModel(factory = GraphsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Graphs") }, navigationIcon = { BackButton(onBack) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RangeSelector(state.range, viewModel::onRangeChange)
            val bounds = state.bounds
            when {
                state.loading -> Unit
                bounds == null -> EmptyChart("Nothing to show for this range")
                else -> ProvideVicoTheme(rememberM3VicoTheme()) { Charts(state, bounds) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSelector(selected: GraphRange, onSelect: (GraphRange) -> Unit) {
    val ranges = GraphRange.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ranges.forEachIndexed { i, range ->
            SegmentedButton(
                selected = range == selected,
                onClick = { onSelect(range) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = ranges.size),
                icon = {},
            ) { Text(range.label, maxLines = 1) }
        }
    }
}

@Composable
private fun Charts(state: GraphsUiState, bounds: ClosedRange<LocalDate>) {
    val s = state.series
    val colors = MaterialTheme.colorScheme
    val macroColors = macroPalette()

    ChartCard("Weight (lbs)") {
        if (s.weight.isEmpty()) {
            EmptyChart("No weight in this range")
        } else {
            DailyChart(
                series = listOf(ChartSeries("Weight", colors.primary, s.weight)),
                bounds = bounds,
                style = ChartStyle.LINES,
                fromZero = false,
                formatValue = { _, v -> "${formatLbs(v)} lbs" },
            )
        }
    }

    ChartCard("Calories (kcal)") {
        if (s.calories.isEmpty()) {
            EmptyChart("No food in this range")
        } else {
            DailyChart(
                series = listOf(ChartSeries("Calories", colors.tertiary, s.calories)),
                bounds = bounds,
                style = ChartStyle.BARS,
                fromZero = true,
                formatValue = { _, v -> "${formatKcal(v)} kcal" },
            )
        }
    }

    ChartCard("Macros (g)") {
        if (s.protein.isEmpty()) {
            EmptyChart("No food in this range")
        } else {
            val macros = listOf(
                ChartSeries("Protein", macroColors[0], s.protein),
                ChartSeries("Carbs", macroColors[1], s.carbs),
                ChartSeries("Fat", macroColors[2], s.fat),
                ChartSeries("Fiber", macroColors[3], s.fiber),
            )
            DailyChart(
                series = macros,
                bounds = bounds,
                style = ChartStyle.LINES,
                fromZero = true,
                formatValue = { i, v -> "${macros[i].label} ${formatGrams(v)} g" },
            )
            Legend(macros)
        }
    }

    ChartCard("Sleep (hours)") {
        if (s.sleepHours.isEmpty()) {
            EmptyChart("No sleep in this range")
        } else {
            DailyChart(
                series = listOf(ChartSeries("Sleep", colors.secondary, s.sleepHours)),
                bounds = bounds,
                style = ChartStyle.BARS,
                fromZero = true,
                formatValue = { _, v -> formatDuration((v * 3_600_000).toLong()) },
            )
        }
    }
}

/** Four macro colors that stay distinct in light and dark themes. */
@Composable
private fun macroPalette(): List<Color> =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        listOf(Color(0xFF8AB4F8), Color(0xFFFDD663), Color(0xFFF28B82), Color(0xFF81C995))
    } else {
        listOf(Color(0xFF1A73E8), Color(0xFFE37400), Color(0xFFD93025), Color(0xFF188038))
    }

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Legend(series: List<ChartSeries>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).background(s.color, CircleShape))
                Text(s.label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyChart(message: String) {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}
