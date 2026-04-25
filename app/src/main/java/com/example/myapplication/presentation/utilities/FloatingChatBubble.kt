package com.example.myapplication.presentation.utilities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import com.example.myapplication.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

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
    val bubbleSizePx = with(density) { 64.dp.toPx() }

    // Bubble position state
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - bubbleSizePx - with(density) { 16.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx * 0.65f) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedElevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "elevation"
    )

    // Smooth edge-snap animation
    var targetX by remember { mutableFloatStateOf(screenWidthPx - bubbleSizePx - with(density) { 16.dp.toPx() }) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "snapX"
    )

    // Pulse ring animation for discoverability
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Pulse ring
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), offsetY.roundToInt()) }
                .size((64 * pulseScale).dp)
                .offset(x = (-((64 * pulseScale - 64) / 2)).dp, y = (-((64 * pulseScale - 64) / 2)).dp)
                .clip(CircleShape)
                .background(Color(0xFF6366F1).copy(alpha = pulseAlpha * 0.3f))
        )

        // Bubble
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), offsetY.roundToInt()) }
                .size(64.dp)
                .shadow(animatedElevation, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            // Snap to nearest edge (animated via animateFloatAsState)
                            val midX = screenWidthPx / 2
                            targetX = if (offsetX + bubbleSizePx / 2 < midX) {
                                with(density) { 8.dp.toPx() }
                            } else {
                                screenWidthPx - bubbleSizePx - with(density) { 8.dp.toPx() }
                            }
                            // Clamp Y
                            offsetY = offsetY.coerceIn(0f, screenHeightPx - bubbleSizePx)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - bubbleSizePx)
                            targetX = offsetX // Follow finger during drag
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
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_ai_chat),
                contentDescription = "Mở AI Chat",
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Tooltip label (shows briefly, optional enhancement)
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt() - with(density) { 20.dp.toPx() }.roundToInt(), offsetY.roundToInt() - with(density) { 36.dp.toPx() }.roundToInt()) }
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
