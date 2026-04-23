package com.example.myapplication.presentation.utilities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * In-app floating chat bubble.
 * Overlays on all screens within the app. Draggable. Tap to open chat.
 * No system permissions required — purely Compose overlay.
 */
@Composable
fun FloatingChatBubble(
    isVisible: Boolean,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val bubbleSizePx = with(density) { 56.dp.toPx() }

    // Bubble position state
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - bubbleSizePx - with(density) { 16.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx * 0.65f) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedElevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "elevation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Bubble
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(56.dp)
                .shadow(animatedElevation, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            // Snap to nearest edge
                            val midX = screenWidthPx / 2
                            offsetX = if (offsetX + bubbleSizePx / 2 < midX) {
                                with(density) { 8.dp.toPx() }  // Snap left
                            } else {
                                screenWidthPx - bubbleSizePx - with(density) { 8.dp.toPx() }  // Snap right
                            }
                            // Clamp Y
                            offsetY = offsetY.coerceIn(0f, screenHeightPx - bubbleSizePx)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - bubbleSizePx)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeightPx - bubbleSizePx)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onOpenChat() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = "Mở AI Chat",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Tooltip label (shows briefly, optional enhancement)
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt() - with(density) { 20.dp.toPx() }.roundToInt(), offsetY.roundToInt() - with(density) { 36.dp.toPx() }.roundToInt()) }
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.inverseSurface)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "AI Chat",
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
