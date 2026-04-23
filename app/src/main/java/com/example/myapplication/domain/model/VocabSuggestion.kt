package com.example.myapplication.domain.model

/**
 * Vocabulary suggestion detected by AI during chat.
 * Used to offer "Save as Flashcard" in the chat UI.
 */
data class VocabSuggestion(
    val word: String,
    val pronunciation: String? = null,
    val partOfSpeech: String = "",
    val definitionEn: String = "",
    val definitionVi: String = "",
    val example: String = "",
    val isSelected: Boolean = true
)
