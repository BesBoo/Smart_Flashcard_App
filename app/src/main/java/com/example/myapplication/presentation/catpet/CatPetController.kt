package com.example.myapplication.presentation.catpet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
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
    private var lastTapTime: Long = 0L
    private var lastEatBubbleTime: Long = 0L

    companion object {
        const val SLEEP_DURATION_MS = 30_000L
        const val SLEEP_COOLDOWN_MS = 60_000L
        const val RANDOM_MIN_MS = 6_000L
        const val RANDOM_MAX_MS = 12_000L
        const val TAP_COOLDOWN_MS = 3_000L
        const val FISH_REACH_THRESHOLD = 0.05f
        const val EAT_BUBBLE_COOLDOWN_MS = 20_000L
        const val EAT_BUBBLE_TEXT = "Meow!!! Ngon quá"
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
                    // Special handling: do not start eating until the cat reaches the fish.
                    if (s.currentAnimation == CatAnimation.CAT_MOVE_TO_FISH) {
                        val distanceToFish = abs(next.fishPositionX - next.positionX)
                        if (distanceToFish <= FISH_REACH_THRESHOLD) {
                            val eatAnim = if (animationQueue.isNotEmpty()) {
                                animationQueue.removeFirst()
                            } else {
                                CatAnimation.EAT_FISH_SEQUENCE
                            }
                            next.copy(
                                currentAnimation = eatAnim,
                                currentFrameIdx = 0,
                                isSleeping = false,
                                facingRight = next.fishPositionX > next.positionX
                            )
                        } else {
                            // Keep walking toward fish until close enough.
                            next.copy(currentFrameIdx = 0)
                        }
                    } else {
                        val nextAnim = if (animationQueue.isNotEmpty()) {
                            animationQueue.removeFirst()
                        } else {
                            TransitionMap.getAutoNext(s.currentAnimation)
                        }
                        val justFinishedEating = s.currentAnimation == CatAnimation.EAT_FISH_SEQUENCE
                        val now = System.currentTimeMillis()
                        val shouldShowEatBubble =
                            justFinishedEating && (now - lastEatBubbleTime >= EAT_BUBBLE_COOLDOWN_MS)
                        if (shouldShowEatBubble) {
                            lastEatBubbleTime = now
                        }

                        // Hide fish only when final eat sequence in queue has finished.
                        val hideFish = justFinishedEating &&
                                animationQueue.none { it == CatAnimation.EAT_FISH_SEQUENCE }
                        next.copy(
                            currentAnimation = nextAnim,
                            currentFrameIdx = 0,
                            isSleeping = nextAnim == CatAnimation.SLEEP_SEQUENCE,
                            showFish = if (hideFish) false else next.showFish,
                            showBubble = if (shouldShowEatBubble) true else next.showBubble,
                            bubbleText = if (shouldShowEatBubble) EAT_BUBBLE_TEXT else next.bubbleText
                        )
                    }
                } else {
                    // Looping → reset to frame 0
                    next.copy(currentFrameIdx = 0)
                }
            } else {
                next.copy(currentFrameIdx = nextFrameIdx)
            }

            // Step-based movement
            val spriteFrame = next.currentAnimation.frames
                .getOrElse(next.currentFrameIdx) { 1 }
            val stepSize = WalkFrameData.getStep(
                animation = next.currentAnimation,
                spriteFrame = spriteFrame,
                frameDurationMs = next.currentAnimation.frameDurationMs
            )
            if (stepSize > 0f) {
                // During CAT_MOVE_TO_FISH, always walk toward the fish
                val toward = if (next.currentAnimation == CatAnimation.CAT_MOVE_TO_FISH) {
                    next.fishPositionX > next.positionX
                } else {
                    next.facingRight
                }

                val dx = if (toward) stepSize else -stepSize
                var newX = next.positionX + dx
                var facing = toward
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

        val next = TransitionMap.pickNext(
            current.currentAnimation,
            canSleep(),
            current.hungerBand
        )

        _state.update {
            it.copy(
                currentAnimation = next,
                currentFrameIdx = 0,
                isSleeping = next == CatAnimation.SLEEP_SEQUENCE
            )
        }
    }

    /**
     * Feed fish reward. Shows fish → cat walks toward it → eats.
     * @param count number of fish to eat sequentially
     * @param celebrate true to add celebrate animation after eating
     */
    fun feedFish(count: Int = 1, celebrate: Boolean = false) {
        if (count <= 0) return

        // Fish appears at a random position different from cat
        val catX = _state.value.positionX
        val fishX = if (catX > 0.5f) {
            Random.nextFloat() * 0.4f         // left half
        } else {
            0.6f + Random.nextFloat() * 0.3f  // right half
        }

        // Face toward fish
        val faceRight = fishX > catX

        // Show fish immediately
        _state.update {
            it.copy(
                showFish = true,
                fishPositionX = fishX,
                facingRight = faceRight
            )
        }

        // Queue: move → eat (× count) → optionally celebrate
        animationQueue.clear()
        animationQueue.addLast(CatAnimation.CAT_MOVE_TO_FISH)
        repeat(count) {
            animationQueue.addLast(CatAnimation.EAT_FISH_SEQUENCE)
        }
        if (celebrate) {
            animationQueue.addLast(CatAnimation.CELEBRATE_SEQUENCE)
        }
    }

    /**
     * Tap interaction — triggers PLAY_ROLL with 3s cooldown.
     */
    fun tapPet() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < TAP_COOLDOWN_MS) return
        if (_state.value.isSleeping) return

        lastTapTime = now
        animationQueue.clear()
        animationQueue.addLast(CatAnimation.PLAY_ROLL)
    }

    /** Show a bubble message above the cat */
    fun showBubble(text: String) {
        _state.update { it.copy(showBubble = true, bubbleText = text) }
    }

    /** Dismiss the bubble */
    fun dismissBubble() {
        _state.update { it.copy(showBubble = false, bubbleText = "") }
    }

    /** Update hunger band (called from outside) */
    fun setHungerBand(band: HungerBand) {
        _state.update { it.copy(hungerBand = band) }
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
