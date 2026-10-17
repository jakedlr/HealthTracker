package com.jakedlr.healthtracker.ui.graphs

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jakedlr.healthtracker.domain.DailyValue
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

/** One plotted series: its legend label, color, and per-day points (days without data absent). */
data class ChartSeries(val label: String, val color: Color, val points: List<DailyValue>)

enum class ChartStyle { LINES, BARS }

/**
 * Chart with one x step per day (x = epoch day) across [bounds], so days without data are gaps.
 * Lines connect across gaps and show a dot per data point; bars are simply absent on empty days.
 *
 * @param fromZero start the y-axis at 0; otherwise pad it around the data.
 * @param formatValue formats a y value for the tap marker, e.g. "182.4 lbs".
 */
@Composable
fun DailyChart(
    series: List<ChartSeries>,
    bounds: ClosedRange<LocalDate>,
    style: ChartStyle,
    fromZero: Boolean,
    formatValue: (seriesIndex: Int, value: Double) -> String,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(series, style) {
        modelProducer.runTransaction {
            when (style) {
                ChartStyle.LINES -> lineModel {
                    series.forEach { s -> series(s.points.map { it.date.toEpochDay() }, s.points.map { it.value }) }
                }
                ChartStyle.BARS -> columnModel {
                    series.forEach { s -> series(s.points.map { it.date.toEpochDay() }, s.points.map { it.value }) }
                }
            }
        }
    }

    val minX = bounds.start.toEpochDay().toDouble()
    val maxX = bounds.endInclusive.toEpochDay().toDouble()
    val spanDays = (maxX - minX).toInt()
    val allValues = series.flatMap { s -> s.points.map { it.value } }
    val (minY, maxY) = if (fromZero) {
        0.0 to ceil(allValues.max() * 1.1).coerceAtLeast(1.0)
    } else {
        floor(allValues.min() - 2) to ceil(allValues.max() + 2)
    }
    val rangeProvider = CartesianLayerRangeProvider.fixed(minX = minX, maxX = maxX, minY = minY, maxY = maxY)

    val dateLabels = remember(spanDays) { dateLabelFormatter(spanDays) }
    val markerDates = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    // About five date labels whatever the span.
    val labelSpacing = (spanDays / 4).coerceAtLeast(1)

    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
            padding = Insets(8.dp, 4.dp),
            background = rememberShapeComponent(
                fill = Fill(MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = RoundedCornerShape(6.dp),
            ),
        ),
        valueFormatter = remember(formatValue) {
            DefaultCartesianMarker.ValueFormatter { _, targets -> markerText(targets, markerDates, formatValue) }
        },
    )

    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Bars shrink with the number of days so they never overlap.
        val barWidth: Dp = (maxWidth / (spanDays + 1) * 0.6f).coerceIn(1.dp, 20.dp)
        val layer = when (style) {
            ChartStyle.LINES -> rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    series.map { s ->
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(Fill(s.color)),
                            areaFill = null,
                            pointProvider = LineCartesianLayer.PointProvider.single(
                                LineCartesianLayer.Point(
                                    rememberShapeComponent(fill = Fill(s.color), shape = CircleShape),
                                    size = 6.dp,
                                ),
                            ),
                        )
                    },
                ),
                rangeProvider = rangeProvider,
            )
            ChartStyle.BARS -> rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    series.map { s ->
                        rememberLineComponent(
                            fill = Fill(s.color),
                            thickness = barWidth,
                            shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                        )
                    },
                ),
                rangeProvider = rangeProvider,
            )
        }

        CartesianChartHost(
            chart = rememberCartesianChart(
                layer,
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = CartesianValueFormatter { _, value, _ ->
                        LocalDate.ofEpochDay(value.toLong()).format(dateLabels)
                    },
                    itemPlacer = remember(labelSpacing) {
                        HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing })
                    },
                ),
                marker = marker,
                getXStep = { _, _, _ -> 1.0 },
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
    }
}

/** Weekday names for a week; month/day for longer spans; with the year beyond about a year. */
private fun dateLabelFormatter(spanDays: Int): DateTimeFormatter = when {
    spanDays <= 7 -> DateTimeFormatter.ofPattern("EEE")
    spanDays <= 366 -> DateTimeFormatter.ofPattern("M/d")
    else -> DateTimeFormatter.ofPattern("M/d/yy")
}

/** e.g. "Thu, Oct 15 · 182.4 lbs", or for several series "… · Protein 120.5 g · Carbs 190.0 g". */
private fun markerText(
    targets: List<CartesianMarker.Target>,
    dates: DateTimeFormatter,
    formatValue: (Int, Double) -> String,
): CharSequence {
    val target = targets.firstOrNull() ?: return ""
    val values = when (target) {
        is LineCartesianLayerMarkerTarget -> target.points.map { it.entry.seriesIndex to it.entry.y }
        is ColumnCartesianLayerMarkerTarget -> target.columns.map { it.entry.seriesIndex to it.entry.y }
        else -> emptyList()
    }
    val date = LocalDate.ofEpochDay(target.x.toLong()).format(dates)
    return (listOf(date) + values.sortedBy { it.first }.map { (i, y) -> formatValue(i, y) }).joinToString(" · ")
}
