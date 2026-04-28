package com.example.myapplication.presentation.utilities

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A simple vertical scrollbar modifier for LazyColumn.
 * Shows a rounded thumb on the right side that fades in/out based on scroll state.
 */
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    color: Color = Color.Gray,
    width: Float = 4f
): Modifier = composed {
    val totalItems = state.layoutInfo.totalItemsCount
    val visibleItems = state.layoutInfo.visibleItemsInfo.size

    // Only show scrollbar when there are more items than visible
    val needsScrollbar = totalItems > visibleItems && totalItems > 0

    val targetAlpha = if (state.isScrollInProgress && needsScrollbar) 1f else if (needsScrollbar) 0.4f else 0f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "scrollbarAlpha"
    )

    drawWithContent {
        drawContent()

        if (alpha > 0f && totalItems > 0 && visibleItems > 0) {
            val viewportHeight = size.height
            val thumbHeight = (visibleItems.toFloat() / totalItems) * viewportHeight
            val minThumbHeight = 32.dp.toPx()
            val actualThumbHeight = thumbHeight.coerceAtLeast(minThumbHeight)

            // Calculate scroll position
            val firstVisibleIndex = state.firstVisibleItemIndex
            val firstVisibleOffset = state.firstVisibleItemScrollOffset
            val scrollRange = totalItems - visibleItems
            val scrollFraction = if (scrollRange > 0) {
                (firstVisibleIndex.toFloat() + firstVisibleOffset.toFloat() /
                        (state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 1f)) / scrollRange
            } else 0f

            val thumbOffset = scrollFraction.coerceIn(0f, 1f) * (viewportHeight - actualThumbHeight)

            drawRoundRect(
                color = color.copy(alpha = alpha * 0.5f),
                topLeft = Offset(size.width - width.dp.toPx() - 2.dp.toPx(), thumbOffset),
                size = Size(width.dp.toPx(), actualThumbHeight),
                cornerRadius = CornerRadius(width.dp.toPx() / 2)
            )
        }
    }
}
