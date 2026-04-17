package com.example.myapplication.domain.model

/**
 * Review quality grades for SM-2 algorithm.
 *
 * Maps to the 4 Vietnamese feedback buttons in the study session UI:
 *   - "Học lại"  → AGAIN (q=0)  — Complete failure, reset card
 *   - "Khó"      → HARD  (q=2)  — Recalled with difficulty
 *   - "Tốt"      → GOOD  (q=3)  — Normal recall
 *   - "Dễ"       → EASY  (q=5)  — Effortless recall
 *
 * @property value   The SM-2 quality score (0, 2, 3, or 5)
 * @property labelVi Vietnamese button label
 * @property labelEn English button label
 */
enum class ReviewQuality(
    val value: Int,
    val labelVi: String,
    val labelEn: String
) {
    AGAIN(0, "Học lại", "Again"),
    HARD(2, "Khó", "Hard"),
    GOOD(3, "Tốt", "Good"),
    EASY(5, "Dễ", "Easy");

    companion object {
        /** Convert raw int quality to enum. Throws if invalid. */
        fun fromValue(value: Int): ReviewQuality =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException(
                    "Invalid quality value: $value. Must be one of: ${entries.map { it.value }}"
                )

        /** Safe conversion — returns null if invalid instead of throwing */
        fun fromValueOrNull(value: Int): ReviewQuality? =
            entries.firstOrNull { it.value == value }

        /** Get label based on language preference */
        fun getLabel(quality: ReviewQuality, language: String = "vi"): String =
            if (language == "vi") quality.labelVi else quality.labelEn
    }
}
