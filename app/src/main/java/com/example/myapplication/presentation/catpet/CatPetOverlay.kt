package com.example.myapplication.presentation.catpet

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * ═══════════════════════════════════════════════════════════
 *  CAT PET OVERLAY
 *  Renders the animated cat sprite on top of the navbar.
 *  Includes: fish reward icon, bubble message, tap interaction.
 * ═══════════════════════════════════════════════════════════
 */
@Composable
fun CatPetOverlay(
    modifier: Modifier = Modifier,
    controller: CatPetController = remember { CatPetController() }
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // ── Local render state ───────────────────────────────
    var currentAnimation by remember { mutableStateOf(CatAnimation.IDLE_VARIATION) }
    var frameIdx by remember { mutableStateOf(0) }
    var positionX by remember { mutableStateOf(0.3f) }
    var facingRight by remember { mutableStateOf(true) }
    var isSleeping by remember { mutableStateOf(false) }
    var showFish by remember { mutableStateOf(false) }
    var fishPositionX by remember { mutableStateOf(0.5f) }
    var showBubble by remember { mutableStateOf(false) }
    var bubbleText by remember { mutableStateOf("") }

    // ── Load cat frame PNGs (cat_1..cat_N) ───────────────
    val frameBitmaps: List<ImageBitmap> = remember {
        val res = context.resources
        val pkg = context.packageName
        (1..SpriteSheet.TOTAL_FRAMES).map { i ->
            val resId = res.getIdentifier("cat_$i", "drawable", pkg)
            BitmapFactory.decodeResource(res, resId).asImageBitmap()
        }
    }

    // ── Load fish icon ───────────────────────────────────
    val fishBitmap: ImageBitmap = remember {
        val res = context.resources
        val pkg = context.packageName
        val resId = res.getIdentifier("fish_small", "drawable", pkg)
        BitmapFactory.decodeResource(res, resId).asImageBitmap()
    }

    // ── Sync state from controller ───────────────────────
    fun syncState() {
        val s = controller.state.value
        currentAnimation = s.currentAnimation
        frameIdx = s.currentFrameIdx
        positionX = s.positionX
        facingRight = s.facingRight
        isSleeping = s.isSleeping
        showFish = s.showFish
        fishPositionX = s.fishPositionX
        showBubble = s.showBubble
        bubbleText = s.bubbleText
    }

    // ── Frame animation timer ────────────────────────────
    LaunchedEffect(Unit) {
        while (true) {
            val anim = currentAnimation
            delay(anim.frameDurationMs)

            controller.tick()
            syncState()
        }
    }

    // ── Random behavior timer ────────────────────────────
    LaunchedEffect(Unit) {
        while (true) {
            delay(controller.nextRandomDelayMs())
            controller.transitionNext()
            syncState()
        }
    }

    // ── Sleep timer ──────────────────────────────────────
    LaunchedEffect(isSleeping) {
        if (isSleeping) {
            delay(CatPetController.SLEEP_DURATION_MS)
            controller.wakeUp()
            syncState()
        }
    }

    // ── Auto-dismiss bubble after 5 seconds ──────────────
    LaunchedEffect(showBubble) {
        if (showBubble) {
            delay(5000L)
            controller.dismissBubble()
            syncState()
        }
    }

    // ── Render ────────────────────────────────────────────
    BoxWithConstraints(modifier = modifier) {
        val containerWidthPx = with(density) { maxWidth.toPx() }

        val catSize = (maxWidth * 0.32f).coerceIn(112.dp, 170.dp)
        val fishSize = catSize * 0.60f

        // Current sprite frame
        val spriteFrame = currentAnimation.frames.getOrElse(frameIdx) { 1 }
        val spriteIdx = (spriteFrame - 1).coerceIn(0, frameBitmaps.size - 1)
        val bitmap = frameBitmaps[spriteIdx]

        // Horizontal positions
        val catSizePx = with(density) { catSize.toPx() }
        val xPx = (positionX * (containerWidthPx - catSizePx)).roundToInt()

        val fishSizePx = with(density) { fishSize.toPx() }
        val fishXPx = (fishPositionX * (containerWidthPx - fishSizePx)).roundToInt()

        // Per-frame Y offset (anchor stabilization)
        val frameYOffsetPx = with(density) {
            FrameOffsets.getOffset(spriteFrame).dp.toPx().roundToInt()
        }

        // Per-frame walk bounce
        val walkBouncePx = with(density) {
            WalkFrameData.getBounce(currentAnimation, spriteFrame).dp.toPx().roundToInt()
        }

        val totalYOffset = frameYOffsetPx + walkBouncePx

        Box(modifier = Modifier.fillMaxSize()) {

            // ── Fish icon ────────────────────────────────
            AnimatedVisibility(
                visible = showFish,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset { IntOffset(fishXPx, 0) }
            ) {
                Image(
                    bitmap = fishBitmap,
                    contentDescription = "Fish reward",
                    modifier = Modifier.size(fishSize)
                )
            }

            // ── Cat sprite ───────────────────────────────
            Image(
                bitmap = bitmap,
                contentDescription = "Cat Pet",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset { IntOffset(xPx, -totalYOffset) }
                    .size(catSize)
                    .graphicsLayer {
                        scaleX = if (facingRight) -1f else 1f
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            controller.tapPet()
                            val s = controller.state.value
                            currentAnimation = s.currentAnimation
                            frameIdx = s.currentFrameIdx
                        }
                    }
            )

            // ── Bubble message ───────────────────────────
            AnimatedVisibility(
                visible = showBubble && bubbleText.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset {
                        // Position bubble above the cat's head
                        val bubbleYOffset = with(density) { catSize.toPx() + 12.dp.toPx() }.roundToInt()
                        IntOffset(
                            x = (xPx - with(density) { 20.dp.toPx() }.roundToInt())
                                .coerceAtLeast(0),
                            y = -(totalYOffset + bubbleYOffset)
                        )
                    }
            ) {
                val isEatBubble = bubbleText == CatPetController.EAT_BUBBLE_TEXT
                if (isEatBubble) {
                    Text(
                        text = bubbleText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = bubbleText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Start,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
