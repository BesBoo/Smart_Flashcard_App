package com.example.myapplication.domain.model

import com.example.myapplication.data.local.entity.DeckEntity
import com.example.myapplication.data.local.entity.FlashcardEntity
import com.example.myapplication.data.local.entity.ReviewLogEntity
import com.example.myapplication.data.local.entity.UserEntity

// ============================================================
// ENTITY → DOMAIN  (Data Layer → Domain Layer)
// ============================================================

/** Convert Room [DeckEntity] to domain [Deck] */
fun DeckEntity.toDomain(cardCount: Int = 0, dueCount: Int = 0): Deck = Deck(
    id = id,
    userId = userId,
    name = name,
    description = description,
    coverImageUrl = coverImageUrl,
    cardCount = cardCount,
    dueCount = dueCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isOwner = isOwner,
    permission = permission,
    ownerName = ownerName,
    shareCode = shareCode,
    isShared = isShared,
    googleSheetUrl = googleSheetUrl
)

/** Convert Room [FlashcardEntity] to domain [Flashcard] */
fun FlashcardEntity.toDomain(): Flashcard = Flashcard(
    id = id,
    userId = userId,
    deckId = deckId,
    frontText = frontText,
    backText = backText,
    exampleText = exampleText,
    pronunciationIpa = pronunciationIpa,
    imageUrl = imageUrl,
    audioUrl = audioUrl,
    sm2 = SM2Data(
        repetition = repetition,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        nextReviewDate = nextReviewDate,
        failCount = failCount,
        totalReviews = totalReviews
    ),
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Convert Room [ReviewLogEntity] to domain [ReviewLog] */
fun ReviewLogEntity.toDomain(): ReviewLog = ReviewLog(
    id = id,
    userId = userId,
    flashcardId = flashcardId,
    quality = ReviewQuality.fromValue(quality),
    responseTimeMs = responseTimeMs,
    reviewedAt = reviewedAt
)

/** Convert Room [UserEntity] to domain [User] */
fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    role = UserRole.fromString(role),
    subscriptionTier = SubscriptionTier.fromString(subscriptionTier),
    aiUsageToday = aiUsageToday,
    isActive = isActive,
    createdAt = createdAt
)

// ============================================================
// DOMAIN → ENTITY  (Domain Layer → Data Layer)
// ============================================================

/** Convert domain [Deck] to Room [DeckEntity] */
fun Deck.toEntity(): DeckEntity = DeckEntity(
    id = id,
    userId = userId,
    name = name,
    description = description,
    coverImageUrl = coverImageUrl,
    isOwner = isOwner,
    permission = permission,
    ownerName = ownerName,
    shareCode = shareCode,
    isShared = isShared,
    googleSheetUrl = googleSheetUrl,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Convert domain [Flashcard] to Room [FlashcardEntity] */
fun Flashcard.toEntity(): FlashcardEntity = FlashcardEntity(
    id = id,
    userId = userId,
    deckId = deckId,
    frontText = frontText,
    backText = backText,
    exampleText = exampleText,
    pronunciationIpa = pronunciationIpa,
    imageUrl = imageUrl,
    audioUrl = audioUrl,
    repetition = sm2.repetition,
    intervalDays = sm2.intervalDays,
    easeFactor = sm2.easeFactor,
    nextReviewDate = sm2.nextReviewDate,
    failCount = sm2.failCount,
    totalReviews = sm2.totalReviews,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Convert domain [ReviewLog] to Room [ReviewLogEntity] */
fun ReviewLog.toEntity(): ReviewLogEntity = ReviewLogEntity(
    id = id,
    userId = userId,
    flashcardId = flashcardId,
    quality = quality.value,
    responseTimeMs = responseTimeMs,
    reviewedAt = reviewedAt
)

// ============================================================
// LIST MAPPERS (convenience extensions)
// ============================================================

fun List<DeckEntity>.toDomainDecks(cardCount: Int = 0, dueCount: Int = 0): List<Deck> =
    map { it.toDomain(cardCount, dueCount) }

fun List<FlashcardEntity>.toDomainFlashcards(): List<Flashcard> =
    map { it.toDomain() }

fun List<ReviewLogEntity>.toDomainReviewLogs(): List<ReviewLog> =
    map { it.toDomain() }
