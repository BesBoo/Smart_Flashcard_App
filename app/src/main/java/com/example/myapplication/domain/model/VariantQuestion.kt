package com.example.myapplication.domain.model

/**
 * A variant-based cloze question for Smart Review mode.
 * Shows a sentence with a blank and morphological variant options.
 */
data class VariantQuestion(
    val sentence: String,          // "This problem is too ______ to solve."
    val baseWord: String,          // "complex"
    val options: List<String>,     // ["complex", "complexity", "complexes", "complexing"]
    val correctIndex: Int,         // 0
    val sourceCardId: String? = null
)
