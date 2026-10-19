package com.jakedlr.healthtracker.ui.training

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jakedlr.healthtracker.domain.moved

/**
 * A column whose rows can be reordered by long-pressing and dragging. Rows between the dragged row and
 * its drop point slide out of the way; on release, [onReorder] gets the whole list in its new order (only if it
 * changed). The new order shows immediately, before [items] catches up. Meant for short lists (no auto-scroll).
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    itemContent: @Composable (item: T, dragging: Boolean) -> Unit,
) {
    // Shown order: follows [items], but takes a drop right away so the row doesn't snap back first.
    // One state object for the whole lifetime, since the drag callbacks below capture it.
    var shown by remember { mutableStateOf(items) }
    LaunchedEffect(items) { shown = items }
    val heights = remember { mutableStateMapOf<Any, Int>() }
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current
    val currentOnReorder by rememberUpdatedState(onReorder)

    fun fromIndex() = shown.indexOfFirst { key(it) == draggingKey }

    // Where the dragged row would land: past each neighbour once it has moved over half of it.
    fun targetIndex(): Int {
        val from = fromIndex()
        if (from < 0) return from
        var target = from
        var covered = 0f
        if (dragOffset > 0) {
            for (i in from + 1 until shown.size) {
                val h = heights[key(shown[i])] ?: break
                if (dragOffset > covered + h / 2f) target = i else break
                covered += h
            }
        } else {
            for (i in from - 1 downTo 0) {
                val h = heights[key(shown[i])] ?: break
                if (-dragOffset > covered + h / 2f) target = i else break
                covered += h
            }
        }
        return target
    }

    Column(modifier) {
        val from = fromIndex()
        val target = if (from >= 0) targetIndex() else -1
        val draggedHeight = draggingKey?.let { heights[it] } ?: 0
        shown.forEachIndexed { index, item ->
            val itemKey = key(item)
            key(itemKey) {
                val isDragged = itemKey == draggingKey
                val shift = when {
                    isDragged -> dragOffset
                    from in 0 until index && index <= target -> -draggedHeight.toFloat()
                    from > index && index >= target && target >= 0 -> draggedHeight.toFloat()
                    else -> 0f
                }
                Box(
                    Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = shift }
                        .onSizeChanged { heights[itemKey] = it.height }
                        .pointerInput(itemKey) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingKey = itemKey
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                },
                                onDragEnd = {
                                    val start = fromIndex()
                                    val end = targetIndex()
                                    if (start >= 0 && end >= 0 && start != end) {
                                        // Hand over the whole order: [items] may not include an earlier drop yet.
                                        shown = shown.moved(start, end)
                                        currentOnReorder(shown)
                                    }
                                    draggingKey = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggingKey = null
                                    dragOffset = 0f
                                },
                            )
                        }
                        // Spacing lives inside each row so measured heights include it.
                        .padding(bottom = spacing),
                ) {
                    itemContent(item, isDragged)
                }
            }
        }
    }
}
