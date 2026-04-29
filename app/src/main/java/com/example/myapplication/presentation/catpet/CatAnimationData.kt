package com.example.myapplication.presentation.catpet

/**
 * ═══════════════════════════════════════════════════════════
 *  CAT SPRITE SHEET DATA
 *  Source: drawable/cat_animation.png (5×5 grid, 25 frames)
 * ═══════════════════════════════════════════════════════════
 */

object SpriteSheet {
    const val COLUMNS = 5
    const val ROWS = 5
    const val TOTAL_FRAMES = 29
}

// ── Per-frame Y offset (dp) to stabilize anchor ──────────
object FrameOffsets {
    val yOffsetDp: List<Float> = listOf(
        0f, -1f, -1f, 2f, 3f,       // 1–5
        -6f, -1f, 4f, 5f, -2f,      // 6–10
        8f, 7f, 10f, -4f, -1f,      // 11–15
        4f, 3f, -6f, 4f, 0f,        // 16–20
        -5f, -1f, -1f, 5f, 6f,      // 21–25
        3f, 2f, -2f, 0f             // 26–29 (eat-fish frames)
    )
    fun getOffset(frameIndex: Int): Float =
        yOffsetDp.getOrElse(frameIndex - 1) { 0f }
}

// ── Per-frame walk step & bounce ─────────────────────────
object WalkFrameData {
    private const val BASE_SPEED_NORM_PER_SEC = 0.12f

    private val locomotionAnimations = setOf(
        CatAnimation.WALK_LOOP,
        CatAnimation.WALK_TO_SLEEP,
        CatAnimation.STRETCH_TO_WALK,
        CatAnimation.WALK_TO_JUMP,
        CatAnimation.IDLE_TO_WALK,
        CatAnimation.WALK_TO_IDLE,
        CatAnimation.CAT_MOVE_TO_FISH
    )

    private val frameStepWeight = mapOf(
        1 to 0.65f,
        2 to 1.00f,
        3 to 0.75f,
        4 to 1.00f,
        23 to 0.90f
    )

    private val animationSpeedMultiplier = mapOf(
        CatAnimation.IDLE_TO_WALK to 0.85f,
        CatAnimation.WALK_TO_IDLE to 0.80f,
        CatAnimation.WALK_TO_SLEEP to 0.80f,
        CatAnimation.STRETCH_TO_WALK to 0.90f,
        CatAnimation.WALK_TO_JUMP to 0.95f
    )

    private val bounceDp = mapOf(
        2 to -2.5f,
        4 to -2.5f
    )

    fun getStep(
        animation: CatAnimation,
        spriteFrame: Int,
        frameDurationMs: Long
    ): Float {
        if (animation !in locomotionAnimations) return 0f

        val frameWeight = frameStepWeight[spriteFrame] ?: return 0f
        val durationSec = frameDurationMs.coerceAtLeast(1L) / 1000f
        val speedScale = animationSpeedMultiplier[animation] ?: 1f
        return BASE_SPEED_NORM_PER_SEC * frameWeight * speedScale * durationSec
    }

    fun getBounce(animation: CatAnimation, spriteFrame: Int): Float {
        if (animation !in locomotionAnimations) return 0f
        return bounceDp[spriteFrame] ?: 0f
    }
}

// ── Movement type ────────────────────────────────────────
enum class CatMovement { WALKING, STATIONARY, SLEEPING }

// ── Animation enum ───────────────────────────────────────
enum class CatAnimation(
    val frames: List<Int>,
    val movement: CatMovement,
    val looping: Boolean,
    val frameDurationMs: Long = 200L,
    val endMovement: CatMovement? = null,
    val minDurationMs: Long = 0L,
    val maxDurationMs: Long = 0L
) {
    IDLE_VARIATION(
        frames = listOf(1,4,3,2,3,2,3,2,3,2,9,9,16,16,9,3,2,3,2,3,2,1),
        movement = CatMovement.STATIONARY,
        looping = true,
        frameDurationMs = 300L
    ),
    WALK_LOOP(
        frames = listOf(1,4,3,2,3,2,3,2,3,2,3,2,3,2,3,2,3,2,3,2,3,2,3,2,3,2,3),
        movement = CatMovement.WALKING,
        looping = true,
        frameDurationMs = 150L
    ),
    WALK_TO_SLEEP(
        frames = listOf(1,4,3,2,3,2,3,2,3,2,9,8,10,20,20,20,20,20,20,20,20,20,20,20),
        movement = CatMovement.WALKING,
        looping = false,
        frameDurationMs = 200L,
        endMovement = CatMovement.SLEEPING
    ),
    STRETCH_TO_WALK(
        frames = listOf(1,4,4,3,2,3,2,3,2,9),
        movement = CatMovement.WALKING,
        looping = false,
        frameDurationMs = 200L
    ),
    PLAY_ROLL(
        frames = listOf(9,11,11,11,11,12,12,12,12,12,11,11,11,13,13,13,13,13,13,11,11,12,12,12,12,12,12,12,12,11,11,13,13,11,12,12,11,13,16),
        movement = CatMovement.STATIONARY,
        looping = true,
        frameDurationMs = 550L
    ),
    BIG_JUMP(
        frames = listOf(9,9,18,14,6,7,23,2,3,2,3,2,3,2,3,9,9,9),
        movement = CatMovement.STATIONARY,
        looping = false,
        frameDurationMs = 300L
    ),
    CLEANING_LOOP(
        frames = listOf(16,17,17,25,19,25,19,25,25,17,17,16,16,16,16,16),
        movement = CatMovement.STATIONARY,
        looping = true,
        frameDurationMs = 500L
    ),
    SIT_LICK_LONG(
        frames = listOf(17,17,25,19,25,19,25,25,25,17,17,17,16),
        movement = CatMovement.STATIONARY,
        looping = true,
        frameDurationMs = 620L,
        minDurationMs = 6000L,
        maxDurationMs = 15000L
    ),
    SLEEP_SEQUENCE(
        frames = listOf(8,10,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20),
        movement = CatMovement.SLEEPING,
        looping = true,
        frameDurationMs = 500L
    ),
    WALK_TO_JUMP(
        frames = listOf(1,4,3,2,3,2,9,18,14,6,7,23,2,3,2,3,1,9,9,9),
        movement = CatMovement.WALKING,
        looping = false,
        frameDurationMs = 160L
    ),
    IDLE_TO_WALK(
        frames = listOf(1,1,4,3,2,3,2,3,2,9,9,9,9,9),
        movement = CatMovement.STATIONARY,
        looping = false,
        frameDurationMs = 250L
    ),
    WALK_TO_IDLE(
        frames = listOf(2,3,2,3,9,9,16,9,3,2,1,1),
        movement = CatMovement.WALKING,
        looping = false,
        frameDurationMs = 180L
    ),
    CELEBRATE_SEQUENCE(
        frames = listOf(9,18,21,22,23,7,9,17,19,25,25,25,17,16),
        movement = CatMovement.STATIONARY,
        looping = false,
        frameDurationMs = 150L
    ),
    WAKE_UP(
        frames = listOf(20,20,20,20,20,10,10,10,10,8,8,8,8,9,9,9,9,9,16,16,1),
        movement = CatMovement.STATIONARY,
        looping = false,
        frameDurationMs = 300L
    ),

    // ── Eat-fish animations (reward) ──────────────────
    /** Walk toward the fish */
    CAT_MOVE_TO_FISH(
        frames = listOf(9,9,3,2,3,2,3,2,3,2,3,2,9,9),
        movement = CatMovement.WALKING,
        looping = false,
        frameDurationMs = 150L
    ),
    /** Eat the fish */
    EAT_FISH_SEQUENCE(
        frames = listOf(28,28,26,26,26,27,27,29,29,8,8,8,8,8),
        movement = CatMovement.STATIONARY,
        looping = false,
        frameDurationMs = 180L
    );
}

// ── Transition Map ───────────────────────────────────────
// Defines which animations can follow each other, with weights.
// Looping anims: pick from weighted list when timer fires.
// Non-looping anims: auto-transition when frames end.

data class WeightedTransition(val animation: CatAnimation, val weight: Int)

object TransitionMap {

    /** Weighted choices for looping animations (timer-triggered) */
    private val weightedTransitions: Map<CatAnimation, List<WeightedTransition>> = mapOf(
        CatAnimation.IDLE_VARIATION to listOf(
            WeightedTransition(CatAnimation.IDLE_TO_WALK, 30),
            WeightedTransition(CatAnimation.PLAY_ROLL, 10),
            WeightedTransition(CatAnimation.CLEANING_LOOP, 25),
            WeightedTransition(CatAnimation.WALK_TO_SLEEP, 5),
            WeightedTransition(CatAnimation.IDLE_VARIATION, 30)
        ),
        CatAnimation.WALK_LOOP to listOf(
            WeightedTransition(CatAnimation.WALK_TO_IDLE, 20),
            WeightedTransition(CatAnimation.WALK_TO_JUMP, 10),
            WeightedTransition(CatAnimation.PLAY_ROLL, 10),
            WeightedTransition(CatAnimation.WALK_TO_SLEEP, 5),
            WeightedTransition(CatAnimation.WALK_LOOP, 55)
        ),
        CatAnimation.PLAY_ROLL to listOf(
            WeightedTransition(CatAnimation.CLEANING_LOOP, 30),
            WeightedTransition(CatAnimation.WALK_LOOP, 30),
            WeightedTransition(CatAnimation.IDLE_VARIATION, 40)
        ),
        CatAnimation.CLEANING_LOOP to listOf(
            WeightedTransition(CatAnimation.SIT_LICK_LONG, 25),
            WeightedTransition(CatAnimation.IDLE_VARIATION, 35),
            WeightedTransition(CatAnimation.WALK_LOOP, 20),
            WeightedTransition(CatAnimation.CLEANING_LOOP, 20)
        ),
        CatAnimation.SIT_LICK_LONG to listOf(
            WeightedTransition(CatAnimation.CLEANING_LOOP, 40),
            WeightedTransition(CatAnimation.IDLE_VARIATION, 60)
        )
    )

    /** Fixed auto-transitions for non-looping animations */
    private val autoTransitions: Map<CatAnimation, CatAnimation> = mapOf(
        CatAnimation.IDLE_TO_WALK to CatAnimation.WALK_LOOP,
        CatAnimation.WALK_TO_IDLE to CatAnimation.IDLE_VARIATION,
        CatAnimation.WALK_TO_JUMP to CatAnimation.IDLE_VARIATION,
        CatAnimation.BIG_JUMP to CatAnimation.IDLE_VARIATION,
        CatAnimation.WALK_TO_SLEEP to CatAnimation.SLEEP_SEQUENCE,
        CatAnimation.SLEEP_SEQUENCE to CatAnimation.WAKE_UP,
        CatAnimation.WAKE_UP to CatAnimation.IDLE_VARIATION,
        CatAnimation.CELEBRATE_SEQUENCE to CatAnimation.IDLE_VARIATION,
        CatAnimation.STRETCH_TO_WALK to CatAnimation.WALK_LOOP,
        // Eat-fish chain
        CatAnimation.CAT_MOVE_TO_FISH to CatAnimation.EAT_FISH_SEQUENCE,
        CatAnimation.EAT_FISH_SEQUENCE to CatAnimation.IDLE_VARIATION
    )

    // ── Hunger-based transition tables ────────────────────
    private val hungerOverrides: Map<HungerBand, Map<CatAnimation, List<WeightedTransition>>> = mapOf(
        HungerBand.LOW to mapOf(
            CatAnimation.IDLE_VARIATION to listOf(
                WeightedTransition(CatAnimation.IDLE_TO_WALK, 25),
                WeightedTransition(CatAnimation.PLAY_ROLL, 20),
                WeightedTransition(CatAnimation.CLEANING_LOOP, 10),
                WeightedTransition(CatAnimation.IDLE_VARIATION, 45)
            )
        ),
        HungerBand.HIGH to mapOf(
            CatAnimation.IDLE_VARIATION to listOf(
                WeightedTransition(CatAnimation.IDLE_TO_WALK, 15),
                WeightedTransition(CatAnimation.CLEANING_LOOP, 40),
                WeightedTransition(CatAnimation.WALK_TO_SLEEP, 10),
                WeightedTransition(CatAnimation.IDLE_VARIATION, 20),
                WeightedTransition(CatAnimation.PLAY_ROLL, 5),
                WeightedTransition(CatAnimation.SIT_LICK_LONG, 10)
            ),
            CatAnimation.WALK_LOOP to listOf(
                WeightedTransition(CatAnimation.WALK_TO_IDLE, 30),
                WeightedTransition(CatAnimation.CLEANING_LOOP, 25),
                WeightedTransition(CatAnimation.WALK_TO_SLEEP, 15),
                WeightedTransition(CatAnimation.WALK_LOOP, 30)
            )
        ),
        HungerBand.VERY_HIGH to mapOf(
            CatAnimation.IDLE_VARIATION to listOf(
                WeightedTransition(CatAnimation.WALK_TO_SLEEP, 50),
                WeightedTransition(CatAnimation.CLEANING_LOOP, 20),
                WeightedTransition(CatAnimation.IDLE_VARIATION, 15),
                WeightedTransition(CatAnimation.IDLE_TO_WALK, 15)
            ),
            CatAnimation.WALK_LOOP to listOf(
                WeightedTransition(CatAnimation.WALK_TO_SLEEP, 50),
                WeightedTransition(CatAnimation.WALK_TO_IDLE, 30),
                WeightedTransition(CatAnimation.WALK_LOOP, 20)
            )
        )
    )

    /** Pick next animation — hunger-aware */
    fun pickNext(
        current: CatAnimation,
        canSleep: Boolean,
        hungerBand: HungerBand = HungerBand.MEDIUM
    ): CatAnimation {
        // Check hunger overrides first, then default table
        val options = hungerOverrides[hungerBand]?.get(current)
            ?: weightedTransitions[current]
            ?: return autoTransitions[current] ?: CatAnimation.IDLE_VARIATION

        // Filter out sleep if cooldown active
        val filtered = if (!canSleep) {
            options.filter { it.animation != CatAnimation.WALK_TO_SLEEP }
        } else options

        if (filtered.isEmpty()) return CatAnimation.IDLE_VARIATION

        val totalWeight = filtered.sumOf { it.weight }
        var roll = kotlin.random.Random.nextInt(totalWeight)
        for (wt in filtered) {
            roll -= wt.weight
            if (roll < 0) return wt.animation
        }
        return CatAnimation.IDLE_VARIATION
    }

    /** Get auto-transition for non-looping animation end */
    fun getAutoNext(current: CatAnimation): CatAnimation =
        autoTransitions[current] ?: CatAnimation.IDLE_VARIATION
}
