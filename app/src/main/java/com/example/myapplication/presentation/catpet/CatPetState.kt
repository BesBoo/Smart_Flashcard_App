package com.example.myapplication.presentation.catpet

/**
 * Immutable state snapshot of the cat pet.
 * Emitted by [CatPetController] and consumed by [CatPetOverlay].
 */
data class CatPetState(
    /** Horizontal position normalized 0f..1f (0 = left edge, 1 = right edge) */
    val positionX: Float = 0.3f,

    /** Currently playing animation */
    val currentAnimation: CatAnimation = CatAnimation.IDLE_VARIATION,

    /** Index into [CatAnimation.frames] — which frame of the animation is showing */
    val currentFrameIdx: Int = 0,

    /** True = cat faces right, False = cat faces left */
    val facingRight: Boolean = true,

    /** True when the cat is in SLEEP_SEQUENCE */
    val isSleeping: Boolean = false,

    /** Monotonically increasing tick counter — used for walk bounce calculation */
    val tickCount: Long = 0L
) {
    /** The actual sprite frame index (1-based) to render */
    val currentSpriteFrame: Int
        get() = currentAnimation.frames.getOrElse(currentFrameIdx) { 1 }
}
