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
    val tickCount: Long = 0L,

    // ── Fish reward state ────────────────────────────
    /** True when a fish icon should be displayed */
    val showFish: Boolean = false,

    /** Fish horizontal position normalized 0f..1f */
    val fishPositionX: Float = 0.5f,

    // ── Bubble message state ─────────────────────────
    /** True when bubble message is visible above cat */
    val showBubble: Boolean = false,

    /** Text content of the bubble */
    val bubbleText: String = "",

    // ── Hunger ───────────────────────────────────────
    /** Current hunger band for UI hints */
    val hungerBand: HungerBand = HungerBand.MEDIUM
) {
    /** The actual sprite frame index (1-based) to render */
    val currentSpriteFrame: Int
        get() = currentAnimation.frames.getOrElse(currentFrameIdx) { 1 }
}
