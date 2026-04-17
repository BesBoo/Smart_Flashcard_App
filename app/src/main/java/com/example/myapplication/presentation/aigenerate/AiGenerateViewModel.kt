package com.example.myapplication.presentation.aigenerate

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.repository.AiRepository
import com.example.myapplication.domain.repository.DeckRepository
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.UserRepository
import com.example.myapplication.domain.usecase.GenerateFlashcardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How the user provides input */
enum class InputMode { TEXT, PDF, DOCX }

/** What the AI should do with the file */
enum class GenerationMode {
    FLASHCARDS,       // Create flashcards from content
    EXTRACT_VOCAB     // Extract vocabulary words from a passage
}

/**
 * A draft card that the user can review/edit before saving.
 */
data class DraftCard(
    val flashcard: Flashcard,
    val isSelected: Boolean = true,
    val isEditing: Boolean = false
)

data class AiGenerateUiState(
    val deck: Deck? = null,
    val inputText: String = "",
    val inputMode: InputMode = InputMode.TEXT,
    val generationMode: GenerationMode = GenerationMode.FLASHCARDS,
    val targetLanguage: String = "vi",
    val selectedFileName: String? = null,
    val selectedFileUri: Uri? = null,
    val isGenerating: Boolean = false,
    /** AI-generated drafts for review (AI-04/05/VOCAB-06) */
    val drafts: List<DraftCard> = emptyList(),
    val isSaved: Boolean = false,
    val error: String? = null,
    /** AI-15: Remaining AI usage count */
    val remainingAiUsage: Int? = null,
    val maxAiUsage: Int = 20
) {
    val selectedCount: Int get() = drafts.count { it.isSelected }
}

@HiltViewModel
class AiGenerateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
    private val userRepository: UserRepository,
    private val generateFlashcardsUseCase: GenerateFlashcardsUseCase,
    private val flashcardRepository: FlashcardRepository,
    private val aiRepository: AiRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val deckId: String = savedStateHandle["deckId"] ?: ""

    private val _uiState = MutableStateFlow(AiGenerateUiState())
    val uiState: StateFlow<AiGenerateUiState> = _uiState.asStateFlow()

    init {
        loadDeck()
        loadRemainingUsage()
    }

    private fun loadDeck() {
        viewModelScope.launch {
            val deck = deckRepository.getDeckById(deckId)
            _uiState.update { it.copy(deck = deck) }
        }
    }

    private fun loadRemainingUsage() {
        viewModelScope.launch {
            try {
                val remaining = aiRepository.getRemainingUsage()
                _uiState.update { it.copy(remainingAiUsage = remaining) }
            } catch (_: Exception) {
                // Non-critical, ignore
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update {
            it.copy(
                inputText = text,
                inputMode = InputMode.TEXT,
                selectedFileName = null,
                selectedFileUri = null
            )
        }
    }

    fun selectFile(uri: Uri, fileName: String, mode: InputMode) {
        _uiState.update {
            it.copy(
                inputMode = mode,
                selectedFileUri = uri,
                selectedFileName = fileName,
                inputText = "",
                error = null,
                generationMode = GenerationMode.EXTRACT_VOCAB
            )
        }
    }

    fun clearFile() {
        _uiState.update {
            it.copy(
                inputMode = InputMode.TEXT,
                selectedFileName = null,
                selectedFileUri = null,
                generationMode = GenerationMode.FLASHCARDS
            )
        }
    }

    fun setGenerationMode(mode: GenerationMode) {
        _uiState.update { it.copy(generationMode = mode) }
    }

    fun setTargetLanguage(lang: String) {
        _uiState.update { it.copy(targetLanguage = lang) }
    }

    /**
     * Generate drafts from AI — does NOT save to DB.
     * User must review and explicitly save (AI-04/05).
     */
    fun generateDrafts() {
        val state = _uiState.value
        if (state.inputMode == InputMode.TEXT && state.inputText.isBlank()) return
        if (state.inputMode != InputMode.TEXT && state.selectedFileUri == null) return

        _uiState.update { it.copy(isGenerating = true, error = null, drafts = emptyList(), isSaved = false) }

        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: throw Exception("Not logged in")
                val deck = state.deck ?: throw Exception("Deck not found")

                val cards = when {
                    state.inputMode == InputMode.TEXT -> {
                        aiRepository.generateFromText(state.inputText, state.targetLanguage, 10)
                    }
                    state.generationMode == GenerationMode.EXTRACT_VOCAB -> {
                        val inputStream = appContext.contentResolver.openInputStream(state.selectedFileUri!!)
                            ?: throw Exception("Cannot read file")
                        inputStream.use { stream ->
                            aiRepository.extractVocabulary(
                                inputStream = stream,
                                fileName = state.selectedFileName ?: "file",
                                targetLanguage = state.targetLanguage,
                                maxWords = 20
                            )
                        }
                    }
                    else -> {
                        val fileType = if (state.inputMode == InputMode.PDF) "pdf" else "docx"
                        val inputStream = appContext.contentResolver.openInputStream(state.selectedFileUri!!)
                            ?: throw Exception("Cannot read file")
                        inputStream.use { stream ->
                            when (fileType) {
                                "pdf" -> aiRepository.generateFromPdf(stream, state.selectedFileName ?: "file.pdf", state.targetLanguage, 10)
                                else -> aiRepository.generateFromDocx(stream, state.selectedFileName ?: "file.docx", state.targetLanguage, 10)
                            }
                        }
                    }
                }

                // Assign deck/user to card but DON'T save yet
                val draftCards = cards.map { card ->
                    DraftCard(flashcard = card.copy(userId = userId, deckId = deck.id))
                }

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        drafts = draftCards
                    )
                }

                // Refresh remaining usage
                loadRemainingUsage()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = e.localizedMessage ?: "Đã xảy ra lỗi"
                    )
                }
            }
        }
    }

    /** Toggle card selection (AI-05 / VOCAB-06) */
    fun toggleDraftSelection(index: Int) {
        _uiState.update { state ->
            val mutable = state.drafts.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = mutable[index].copy(isSelected = !mutable[index].isSelected)
            }
            state.copy(drafts = mutable)
        }
    }

    /** Edit a draft card's content (AI-05) */
    fun editDraft(index: Int, frontText: String, backText: String, exampleText: String) {
        _uiState.update { state ->
            val mutable = state.drafts.toMutableList()
            if (index in mutable.indices) {
                val updated = mutable[index].flashcard.copy(
                    frontText = frontText,
                    backText = backText,
                    exampleText = exampleText
                )
                mutable[index] = mutable[index].copy(flashcard = updated)
            }
            state.copy(drafts = mutable)
        }
    }

    /** Remove a draft card */
    fun removeDraft(index: Int) {
        _uiState.update { state ->
            val mutable = state.drafts.toMutableList()
            if (index in mutable.indices) mutable.removeAt(index)
            state.copy(drafts = mutable)
        }
    }

    /** Save selected drafts to DB (AI-04 final step) */
    fun saveSelectedDrafts() {
        viewModelScope.launch {
            try {
                val selectedCards = _uiState.value.drafts
                    .filter { it.isSelected }
                    .map { it.flashcard }

                selectedCards.forEach { flashcardRepository.createFlashcard(it) }
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Lỗi khi lưu") }
            }
        }
    }
}
