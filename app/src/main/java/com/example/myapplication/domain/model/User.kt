package com.example.myapplication.domain.model

/**
 * Domain model for a User.
 * Clean representation without Room annotations or password hash
 * (password is never stored/exposed in the domain layer).
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val role: UserRole = UserRole.USER,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val preferredLanguage: String = "vi",
    val aiUsageToday: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** True if user is on the free tier */
    val isFreeUser: Boolean get() = subscriptionTier == SubscriptionTier.FREE

    /** True if user is on the premium tier */
    val isPremium: Boolean get() = subscriptionTier == SubscriptionTier.PREMIUM

    val isAdmin: Boolean get() = role == UserRole.ADMIN
}

/** Application role from server: "user" | "admin". */
enum class UserRole {
    USER,
    ADMIN;

    fun toApiString(): String = when (this) {
        USER -> "user"
        ADMIN -> "admin"
    }

    companion object {
        fun fromString(value: String): UserRole =
            when (value.lowercase()) {
                "admin" -> ADMIN
                else -> USER
            }
    }
}

/**
 * Subscription tiers with their AI usage limits.
 */
enum class SubscriptionTier(
    val textGenLimit: Int,
    val fileGenLimit: Int,
    val exampleLimit: Int,
    val tutorMsgLimit: Int
) {
    FREE(
        textGenLimit = 20,
        fileGenLimit = 5,
        exampleLimit = 20,
        tutorMsgLimit = 10
    ),
    PREMIUM(
        textGenLimit = Int.MAX_VALUE,
        fileGenLimit = Int.MAX_VALUE,
        exampleLimit = Int.MAX_VALUE,
        tutorMsgLimit = Int.MAX_VALUE
    );

    companion object {
        fun fromString(value: String): SubscriptionTier =
            when (value.lowercase()) {
                "premium" -> PREMIUM
                else -> FREE
            }
    }
}
