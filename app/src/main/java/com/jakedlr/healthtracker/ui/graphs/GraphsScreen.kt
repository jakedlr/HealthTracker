package com.jakedlr.healthtracker.ui.graphs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.domain.DailyValue
import com.jakedlr.healthtracker.ui.BackButton
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Weight (lbs)", style = MaterialTheme.typography.titleMedium)
            when {
                state.loading -> Unit
                state.weight.isEmpty() -> EmptyChart("No weight entries yet")
                else -> DailyLineChart(state.weight, state.bounds!!)
            }
        }
    }
}

@Composable
private fun EmptyChart(message: String) {
    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Line chart with one x unit per day (epoch day), so days without data are horizontal gaps
 * between points rather than zeros.
 */
@Composable
private fun DailyLineChart(points: List<DailyValue>, bounds: ClosedRange<LocalDate>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        modelProducer.runTransaction {
            lineModel {
                series(
                    x = points.map { it.date.toEpochDay() },
                    y = points.map { it.value },
                )
            }
        }
    }

    val minX = bounds.start.toEpochDay().toDouble()
    val maxX = bounds.endInclusive.toEpochDay().toDouble()
    // Pad the y-axis around the data instead of starting at zero, so small changes are visible.
    val minY = floor(points.minOf { it.value } - 2)
    val maxY = ceil(points.maxOf { it.value } + 2)
    // Aim for roughly five date labels whatever the span.
    val labelSpacing = ((maxX - minX) / 4).toInt().coerceAtLeast(1)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
    val color = MaterialTheme.colorScheme.primary

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(Fill(color)),
                        areaFill = null,
                        pointProvider = LineCartesianLayer.PointProvider.single(
                            LineCartesianLayer.Point(
                                rememberShapeComponent(fill = Fill(color), shape = CircleShape),
                                size = 6.dp,
                            ),
                        ),
                    ),
                ),
                rangeProvider = CartesianLayerRangeProvider.fixed(minX = minX, maxX = maxX, minY = minY, maxY = maxY),
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    LocalDate.ofEpochDay(value.toLong()).format(dateFormatter)
                },
                itemPlacer = remember(labelSpacing) {
                    HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing })
                },
            ),
            getXStep = { _, _, _ -> 1.0 },
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        modifier = Modifier.fillMaxWidth().height(240.dp),
    )
}
