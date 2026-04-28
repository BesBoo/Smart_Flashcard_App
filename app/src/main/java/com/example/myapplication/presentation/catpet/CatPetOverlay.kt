package com.example.myapplication.presentation.catpet

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * ═══════════════════════════════════════════════════════════
 *  CAT PET OVERLAY
 *  Renders the animated cat sprite on top of the navbar.
 *  Applies per-frame Y offsets for stable anchor point.
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

    // ── Load 32 individual frame PNGs (cat_1..cat_32) ────
    val frameBitmaps: List<ImageBitmap> = remember {
        val res = context.resources
        val pkg = context.packageName
        (1..SpriteSheet.TOTAL_FRAMES).map { i ->
            val resId = res.getIdentifier("cat_$i", "drawable", pkg)
            BitmapFactory.decodeResource(res, resId).asImageBitmap()
        }
    }

    // ── Frame animation timer ────────────────────────────
    LaunchedEffect(Unit) {
        while (true) {
            val anim = currentAnimation
            delay(anim.frameDurationMs)

            controller.tick()
            val s = controller.state.value

            currentAnimation = s.currentAnimation
            frameIdx = s.currentFrameIdx
            positionX = s.positionX
            facingRight = s.facingRight
            isSleeping = s.isSleeping
        }
    }

    // ── Random behavior timer ────────────────────────────
    LaunchedEffect(Unit) {
        while (true) {
            delay(controller.nextRandomDelayMs())
            controller.transitionNext()

            val s = controller.state.value
            currentAnimation = s.currentAnimation
            frameIdx = s.currentFrameIdx
            isSleeping = s.isSleeping
        }
    }

    // ── Sleep timer ──────────────────────────────────────
    LaunchedEffect(isSleeping) {
        if (isSleeping) {
            delay(CatPetController.SLEEP_DURATION_MS)
            controller.wakeUp()

            val s = controller.state.value
            currentAnimation = s.currentAnimation
            frameIdx = s.currentFrameIdx
            isSleeping = s.isSleeping
        }
    }

    // ── Render ────────────────────────────────────────────
    BoxWithConstraints(modifier = modifier) {
        val containerWidthPx = with(density) { maxWidth.toPx() }

        // Keep cat larger and readable on top of navbar across phones/tablets.
        val catSize = (maxWidth * 0.28f).coerceIn(98.dp, 152.dp)

        // Current sprite frame
        val spriteFrame = currentAnimation.frames.getOrElse(frameIdx) { 1 }
        val spriteIdx = (spriteFrame - 1).coerceIn(0, frameBitmaps.size - 1)
        val bitmap = frameBitmaps[spriteIdx]

        // Horizontal position
        val catSizePx = with(density) { catSize.toPx() }
        val xPx = (positionX * (containerWidthPx - catSizePx)).roundToInt()

        // ── Per-frame Y offset (anchor stabilization) ──
        val frameYOffsetPx = with(density) {
            FrameOffsets.getOffset(spriteFrame).dp.toPx().roundToInt()
        }

        // ── Per-frame walk bounce (synced with foot contact) ──
        val walkBouncePx = with(density) {
            WalkFrameData.getBounce(currentAnimation, spriteFrame).dp.toPx().roundToInt()
        }

        // Total Y offset: anchor + walk bounce
        val totalYOffset = frameYOffsetPx + walkBouncePx

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = bitmap,
                contentDescription = "Cat Pet",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset { IntOffset(xPx, -totalYOffset) }
                    .size(catSize)
                    .graphicsLayer {
                        // Sprite naturally faces LEFT in the sheet
                        // Flip when facing right
                        scaleX = if (facingRight) -1f else 1f
                    }
            )
        }
    }
}
