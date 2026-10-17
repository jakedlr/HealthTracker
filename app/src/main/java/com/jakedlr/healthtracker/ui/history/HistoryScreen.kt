package com.jakedlr.healthtracker.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.domain.DayMarks
import com.jakedlr.healthtracker.domain.calendarGrid
import com.jakedlr.healthtracker.ui.BackButton
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.launch

/** Dot colors; they match the graph colors (weight = primary, food = tertiary, sleep = secondary). */
private data class MarkColors(val weight: Color, val food: Color, val sleep: Color)

@Composable
private fun markColors() = MaterialTheme.colorScheme.let { MarkColors(it.primary, it.tertiary, it.secondary) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("History") }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        if (state.loading) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            MonthPager(state, viewModel, onDayClick)
            Spacer(Modifier.height(16.dp))
            Legend()
        }
    }
}

@Composable
private fun MonthPager(state: HistoryUiState, viewModel: HistoryViewModel, onDayClick: (LocalDate) -> Unit) {
    val pageCount = (ChronoUnit.MONTHS.between(state.firstMonth, state.currentMonth) + 1).toInt()
    // Opens on the current month (the last page); swipe right to go back in time.
    val pagerState = rememberPagerState(initialPage = pageCount - 1) { pageCount }
    val scope = rememberCoroutineScope()
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val monthTitle = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }

    val shownMonth = state.firstMonth.plusMonths(pagerState.currentPage.toLong())
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
            enabled = pagerState.currentPage > 0,
        ) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
        Text(
            shownMonth.format(monthTitle),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        TextButton(
            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            enabled = pagerState.currentPage < pageCount - 1,
        ) { Text("›", style = MaterialTheme.typography.headlineSmall) }
    }
    WeekdayHeader(firstDayOfWeek)

    HorizontalPager(state = pagerState, verticalAlignment = Alignment.Top) { page ->
        val month = state.firstMonth.plusMonths(page.toLong())
        val marks by remember(month) { viewModel.marksFor(month) }.collectAsStateWithLifecycle(emptyMap())
        MonthGrid(month, firstDayOfWeek, state.today, marks, onDayClick)
    }
}

@Composable
private fun WeekdayHeader(firstDayOfWeek: DayOfWeek) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        repeat(7) { i ->
            Text(
                firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate,
    marks: Map<LocalDate, DayMarks>,
    onDayClick: (LocalDate) -> Unit,
) {
    val cells = remember(month, firstDayOfWeek) { calendarGrid(month, firstDayOfWeek) }
    val colors = markColors()
    Column(Modifier.fillMaxWidth()) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(0.9f).padding(2.dp)) {
                        if (date != null) {
                            DayCell(date, today, marks[date] ?: DayMarks(), colors, onDayClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    today: LocalDate,
    marks: DayMarks,
    colors: MarkColors,
    onDayClick: (LocalDate) -> Unit,
) {
    val future = date > today
    val shape = RoundedCornerShape(8.dp)
    var modifier = Modifier.fillMaxSize().clip(shape)
    if (date == today) modifier = modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape)
    modifier = if (future) modifier.alpha(0.35f) else modifier.clickable { onDayClick(date) }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.height(10.dp).padding(top = 3.dp),
        ) {
            if (marks.weight) Dot(colors.weight)
            if (marks.food) Dot(colors.food)
            if (marks.sleep) Dot(colors.sleep)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(6.dp).background(color, CircleShape))
}

@Composable
private fun Legend() {
    val colors = markColors()
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        listOf("Weight" to colors.weight, "Food" to colors.food, "Sleep" to colors.sleep).forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Dot(color)
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
