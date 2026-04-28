package com.example.myapplication.presentation.catpet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/**
 * ═══════════════════════════════════════════════════════════
 *  CAT PET CONTROLLER
 *  Transition-based state machine. Each animation has a list
 *  of allowed next animations with weights.
 * ═══════════════════════════════════════════════════════════
 */
class CatPetController {

    private val _state = MutableStateFlow(CatPetState())
    val state: StateFlow<CatPetState> = _state.asStateFlow()

    private val animationQueue = ArrayDeque<CatAnimation>()
    private var lastSleepEndTime: Long = 0L

    companion object {
        const val SLEEP_DURATION_MS = 30_000L
        const val SLEEP_COOLDOWN_MS = 60_000L
        const val RANDOM_MIN_MS = 6_000L
        const val RANDOM_MAX_MS = 12_000L
    }

    /**
     * Advance one frame. Called once per [CatAnimation.frameDurationMs].
     */
    fun tick() {
        _state.update { s ->
            var next = s.copy(tickCount = s.tickCount + 1)

            // Queue interrupts looping animations
            if (animationQueue.isNotEmpty() && s.currentAnimation.looping) {
                val queued = animationQueue.removeFirst()
                return@update next.copy(
                    currentAnimation = queued,
                    currentFrameIdx = 0,
                    isSleeping = queued == CatAnimation.SLEEP_SEQUENCE
                )
            }

            // Advance frame
            val maxIdx = s.currentAnimation.frames.size - 1
            val nextFrameIdx = s.currentFrameIdx + 1

            next = if (nextFrameIdx > maxIdx) {
                // Non-looping ended → auto-transition
                if (!s.currentAnimation.looping) {
                    val nextAnim = if (animationQueue.isNotEmpty()) {
                        animationQueue.removeFirst()
                    } else {
                        TransitionMap.getAutoNext(s.currentAnimation)
                    }
                    next.copy(
                        currentAnimation = nextAnim,
                        currentFrameIdx = 0,
                        isSleeping = nextAnim == CatAnimation.SLEEP_SEQUENCE
                    )
                } else {
                    // Looping → reset to frame 0
                    next.copy(currentFrameIdx = 0)
                }
            } else {
                next.copy(currentFrameIdx = nextFrameIdx)
            }

            // Step-based movement derived from the rendered frame itself.
            val spriteFrame = next.currentAnimation.frames
                .getOrElse(next.currentFrameIdx) { 1 }
            val stepSize = WalkFrameData.getStep(
                animation = next.currentAnimation,
                spriteFrame = spriteFrame,
                frameDurationMs = next.currentAnimation.frameDurationMs
            )
            if (stepSize > 0f) {
                val dx = if (next.facingRight) stepSize else -stepSize
                var newX = next.positionX + dx
                var facing = next.facingRight
                if (newX >= 1f) { newX = 1f; facing = false }
                else if (newX <= 0f) { newX = 0f; facing = true }
                next = next.copy(positionX = newX, facingRight = facing)
            }

            next
        }
    }

    /**
     * Transition to next animation using weighted transition map.
     */
    fun transitionNext() {
        val current = _state.value
        if (!current.currentAnimation.looping) return
        if (current.isSleeping) return
        if (animationQueue.isNotEmpty()) return

        val next = TransitionMap.pickNext(current.currentAnimation, canSleep())

        _state.update {
            it.copy(
                currentAnimation = next,
                currentFrameIdx = 0,
                isSleeping = next == CatAnimation.SLEEP_SEQUENCE
            )
        }
    }

    fun celebrate() {
        animationQueue.addLast(CatAnimation.CELEBRATE_SEQUENCE)
    }

    fun wakeUp() {
        lastSleepEndTime = System.currentTimeMillis()
        _state.update {
            it.copy(
                currentAnimation = CatAnimation.WAKE_UP,
                currentFrameIdx = 0,
                isSleeping = false
            )
        }
    }

    fun nextRandomDelayMs(): Long =
        Random.nextLong(RANDOM_MIN_MS, RANDOM_MAX_MS + 1)

    private fun canSleep(): Boolean =
        System.currentTimeMillis() - lastSleepEndTime >= SLEEP_COOLDOWN_MS
}
