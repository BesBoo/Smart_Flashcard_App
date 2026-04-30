package com.example.myapplication.presentation.catpet

import android.content.Context
import android.content.SharedPreferences

/**
 * ═══════════════════════════════════════════════════════════
 *  CAT HUNGER MANAGER
 *  Persists hunger state via SharedPreferences.
 *  Hunger increases over time, decreases when user reviews cards.
 * ═══════════════════════════════════════════════════════════
 */
class CatHungerManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "cat_pet_prefs"
        private const val KEY_HUNGER = "hunger_level"
        private const val KEY_LAST_UPDATE = "last_update_ms"
        private const val KEY_LAST_BUBBLE = "last_bubble_ms"
        private const val KEY_PENDING_FISH = "pending_fish_count"
        private const val KEY_REVIEW_PROGRESS = "review_progress_cards"

        /** Hunger increases +3 every 3 hours */
        private const val HUNGER_PER_3H = 3
        private const val THREE_HOURS_MS = 3 * 60 * 60 * 1000L

        /** Hunger decreases -5 per card reviewed */
        private const val HUNGER_PER_CARD = 5

        /** Bubble message cooldown: 4 hours */
        private const val BUBBLE_COOLDOWN_MS = 4 * 60 * 60 * 1000L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Current hunger level 0..100 */
    var hunger: Int
        get() = prefs.getInt(KEY_HUNGER, 30)
        private set(value) {
            prefs.edit().putInt(KEY_HUNGER, value.coerceIn(0, 100)).apply()
        }


    /** Number of fish rewards waiting to be consumed by animation */
    var pendingFish: Int
        get() = prefs.getInt(KEY_PENDING_FISH, 0)
        private set(value) {
            prefs.edit().putInt(KEY_PENDING_FISH, value.coerceAtLeast(0)).apply()
        }

    /** Cards reviewed since last fish payout */
    private var reviewProgress: Int
        get() = prefs.getInt(KEY_REVIEW_PROGRESS, 0)
        set(value) {
            prefs.edit().putInt(KEY_REVIEW_PROGRESS, value.coerceAtLeast(0)).apply()
        }

    /**
     * Called when app opens or resumes.
     * Calculates hunger increase based on elapsed time.
     */
    fun init() {
        val now = System.currentTimeMillis()
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, now)
        val elapsed = (now - lastUpdate).coerceAtLeast(0)

        // +3 hunger per 3 hours elapsed
        val periods = (elapsed / THREE_HOURS_MS).toInt()
        if (periods > 0) {
            hunger = (hunger + periods * HUNGER_PER_3H).coerceAtMost(100)
        }

        prefs.edit().putLong(KEY_LAST_UPDATE, now).apply()
    }

    /**
     * Called each time a card is reviewed (answered).
     * Decreases hunger by 5.
     */
    fun onCardReviewed() {
        hunger = (hunger - HUNGER_PER_CARD).coerceAtLeast(0)
        reviewProgress += 1

        // Every 5 reviewed cards grants 1 fish.
        val fishEarned = reviewProgress / 5
        if (fishEarned > 0) {
            pendingFish += fishEarned
            reviewProgress %= 5
        }

        prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
    }

    /**
     * Called when a study session completes.
     * @param cardsCount total cards reviewed in this session
     * @return the reward type earned
     */
    fun onSessionComplete(cardsCount: Int): RewardType {
        val reward = when {
            cardsCount >= 20 -> RewardType.CELEBRATE
            cardsCount >= 15 -> RewardType.FEAST
            cardsCount >= 10 -> RewardType.BIG_FISH
            cardsCount >= 5  -> RewardType.SMALL_FISH
            else             -> RewardType.NONE
        }
        return reward
    }

    /** Consume one pending fish (called by controller when starting eat animation) */
    fun consumeOneFish() {
        pendingFish = (pendingFish - 1).coerceAtLeast(0)
    }

    /** Atomically take all pending fish and clear storage. */
    fun takePendingFish(): Int {
        val count = pendingFish
        if (count > 0) {
            pendingFish = 0
        }
        return count
    }

    /** Get current hunger band for animation selection */
    fun getHungerBand(): HungerBand = when {
        hunger <= 20 -> HungerBand.LOW
        hunger <= 50 -> HungerBand.MEDIUM
        hunger <= 80 -> HungerBand.HIGH
        else         -> HungerBand.VERY_HIGH
    }

    /** Check if bubble message should be shown (4h cooldown) */
    fun shouldShowBubble(): Boolean {
        val lastShown = prefs.getLong(KEY_LAST_BUBBLE, 0L)
        return System.currentTimeMillis() - lastShown >= BUBBLE_COOLDOWN_MS
    }

    /** Mark bubble as shown (resets cooldown) */
    fun markBubbleShown() {
        prefs.edit().putLong(KEY_LAST_BUBBLE, System.currentTimeMillis()).apply()
    }

    /** Get context-aware bubble message */
    fun getBubbleMessage(dueCards: Int): String = when {
        hunger > 80 -> "Đói quá... cho mình ăn với 😿"
        hunger > 50 -> "Mình đang đói, ôn vài thẻ nhé? 🐟"
        dueCards > 0 -> "Hôm nay còn $dueCards thẻ, ôn nhẹ nhé!"
        hunger <= 20 -> "No rồi, cảm ơn bạn! 😸"
        else -> "Meow! Học cùng mình nhé! 🐱"
    }
}

enum class HungerBand { LOW, MEDIUM, HIGH, VERY_HIGH }

enum class RewardType { NONE, SMALL_FISH, BIG_FISH, FEAST, CELEBRATE }
