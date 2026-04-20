package com.example.myapplication.presentation.flashcardeditor

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.repository.AiRepository
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import com.example.myapplication.data.remote.api.SharedImageApi
import com.example.myapplication.data.remote.dto.ShareImageRequest
import com.example.myapplication.data.remote.dto.SharedImageDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

data class FlashcardEditorUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val frontText: String = "",
    val backText: String = "",
    val exampleText: String = "",
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isGeneratingImage: Boolean = false,
    val isGeneratingExample: Boolean = false,
    val isTtsReady: Boolean = false,
    val error: String? = null,
    // Polysemy — auto-triggered inline
    val isAnalyzing: Boolean = false,
    val analysisResult: com.example.myapplication.domain.repository.WordAnalysisResult? = null,
    val selectedSense: com.example.myapplication.domain.repository.WordSenseItem? = null,
    val senseSaved: Boolean = false,
    val savedSenseKeys: Set<String> = emptySet(),
    // Community image suggestions
    val suggestedImages: List<SharedImageDto> = emptyList(),
    val isLoadingSuggestions: Boolean = false
)

@HiltViewModel
class FlashcardEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val flashcardRepository: FlashcardRepository,
    private val aiRepository: AiRepository,
    private val sharedImageApi: SharedImageApi,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val deckId: String = savedStateHandle["deckId"] ?: ""
    private val cardId: String = savedStateHandle["cardId"] ?: "new"
    private val isEditMode = cardId != "new"

    private val _uiState = MutableStateFlow(FlashcardEditorUiState())
    val uiState: StateFlow<FlashcardEditorUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var analyzeJob: Job? = null
    private var lastAnalyzedWord: String = ""
    private var suggestJob: Job? = null

    init {
        if (isEditMode) loadCard() else _uiState.update { it.copy(isLoading = false) }
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _uiState.update { it.copy(isTtsReady = true) }
            }
        }
    }

    private fun loadCard() {
        viewModelScope.launch {
            try {
                val card = flashcardRepository.getFlashcardById(cardId)
                if (card != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true,
                            frontText = card.frontText,
                            backText = card.backText,
                            exampleText = card.exampleText ?: "",
                            imageUrl = card.imageUrl,
                            audioUrl = card.audioUrl
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Không tìm thấy thẻ") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun updateFront(text: String) {
        _uiState.update { it.copy(frontText = text) }
        // Clear stale analysis when text changes significantly
        val trimmed = text.trim().lowercase()
        if (_uiState.value.analysisResult != null &&
            _uiState.value.analysisResult!!.lemma.lowercase() != trimmed) {
            _uiState.update { it.copy(analysisResult = null) }
        }

        // Fetch community image suggestions (debounced)
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(600L)
            fetchSuggestedImages(trimmed)
        }
    }
    fun updateBack(text: String) { _uiState.update { it.copy(backText = text) } }
    fun updateExample(text: String) { _uiState.update { it.copy(exampleText = text) } }

    /**
     * Manually trigger polysemy analysis when user clicks the button.
     */
    fun analyzeWord() {
        val trimmed = _uiState.value.frontText.trim().lowercase()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(error = "Nhập ít nhất 2 ký tự để phân tích") }
            return
        }
        if (trimmed == lastAnalyzedWord && _uiState.value.analysisResult != null) {
            return // Already analyzed this word
        }

        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            autoAnalyze(trimmed)
        }

        // Fetch community image suggestions (debounced)
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(600L)
            fetchSuggestedImages(trimmed)
        }
    }

    /**
     * Cancel any pending analysis — called before other AI actions
     * to free up API quota.
     */
    private fun cancelAnalysis() {
        analyzeJob?.cancel()
        analyzeJob = null
        if (_uiState.value.isAnalyzing) {
            _uiState.update { it.copy(isAnalyzing = false) }
        }
    }

    private suspend fun autoAnalyze(word: String) {
        _uiState.update { it.copy(isAnalyzing = true) }
        try {
            val back = _uiState.value.backText
            val result = aiRepository.analyzeWord(
                word = word,
                definition = back.ifBlank { word },
                context = _uiState.value.exampleText.ifBlank { null }
            )
            lastAnalyzedWord = word
            _uiState.update { it.copy(isAnalyzing = false, analysisResult = result) }
        } catch (_: kotlinx.coroutines.CancellationException) {
            throw kotlinx.coroutines.CancellationException()
        } catch (_: Exception) {
            _uiState.update { it.copy(isAnalyzing = false) }
        }
    }

    /** Called when user picks an image from gallery */
    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            try {
                // Copy image to app internal storage
                val imagesDir = File(context.filesDir, "card_images")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                val destFile = File(imagesDir, "${UUID.randomUUID()}.jpg")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                _uiState.update { it.copy(imageUrl = destFile.absolutePath) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Không thể tải ảnh: ${e.localizedMessage}") }
            }
        }
    }

    /** Remove the current image */
    fun removeImage() {
        _uiState.update { it.copy(imageUrl = null) }
    }

    /** Generate image using AI: calls backend which searches Wikimedia for relevant images */
    fun generateImageWithAi() {
        cancelAnalysis() // Free up rate limit for this action
        val front = _uiState.value.frontText
        val back = _uiState.value.backText
        if (front.isBlank()) {
            _uiState.update { it.copy(error = "Nhập mặt trước trước khi tạo ảnh AI") }
            return
        }

        _uiState.update { it.copy(isGeneratingImage = true, error = null) }

        viewModelScope.launch {
            try {
                // Call backend API which uses Gemini + Wikimedia to find relevant image
                val imageUrl = aiRepository.generateImageUrl(front, back.ifBlank { front })

                if (imageUrl != null) {
                    _uiState.update {
                        it.copy(
                            isGeneratingImage = false,
                            imageUrl = imageUrl
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isGeneratingImage = false,
                            error = "Không tìm thấy ảnh phù hợp. Hãy thử chọn ảnh từ thư viện."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isGeneratingImage = false, error = "Không thể tạo ảnh: ${e.localizedMessage}")
                }
            }
        }
    }

    /** Generate example using AI */
    fun generateExampleWithAi() {
        cancelAnalysis() // Free up rate limit for this action
        val front = _uiState.value.frontText
        val back = _uiState.value.backText
        if (front.isBlank()) {
            _uiState.update { it.copy(error = "Nhập mặt trước trước khi tạo ví dụ") }
            return
        }

        _uiState.update { it.copy(isGeneratingExample = true, error = null) }

        viewModelScope.launch {
            try {
                val example = aiRepository.generateExample(front, back)
                _uiState.update { it.copy(exampleText = example, isGeneratingExample = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isGeneratingExample = false, error = "Không thể tạo ví dụ: ${e.localizedMessage}")
                }
            }
        }
    }

    /** Play TTS pronunciation for front text */
    fun speakFront() {
        val text = _uiState.value.frontText
        if (text.isBlank() || tts == null) return

        // Auto-detect language: if text contains Vietnamese chars → vi, else → en
        val locale = if (text.any { it in 'ả'..'ỹ' || it in 'Ả'..'Ỹ' || it == 'đ' || it == 'Đ' })
            Locale("vi", "VN")
        else
            Locale.US

        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "front_tts")
    }

    /** Play TTS pronunciation for back text */
    fun speakBack() {
        val text = _uiState.value.backText
        if (text.isBlank() || tts == null) return

        val locale = if (text.any { it in 'ả'..'ỹ' || it in 'Ả'..'Ỹ' || it == 'đ' || it == 'Đ' })
            Locale("vi", "VN")
        else
            Locale.US

        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "back_tts")
    }

    fun save() {
        val state = _uiState.value
        if (state.frontText.isBlank() || state.backText.isBlank()) {
            _uiState.update { it.copy(error = "Mặt trước và mặt sau không được để trống") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val userId = userRepository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update { it.copy(isSaving = false, error = "Bạn chưa đăng nhập") }
                    return@launch
                }

                if (isEditMode) {
                    val existing = flashcardRepository.getFlashcardById(cardId)
                    if (existing != null) {
                        flashcardRepository.updateFlashcard(
                            existing.copy(
                                frontText = state.frontText,
                                backText = state.backText,
                                exampleText = state.exampleText.ifBlank { null },
                                imageUrl = state.imageUrl,
                                audioUrl = state.audioUrl,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                } else {
                    // Check daily card creation limit
                    val prefs = context.getSharedPreferences("study_settings", Context.MODE_PRIVATE)
                    val maxNewPerDay = prefs.getInt("new_cards_per_day", 40)
                    val todayStart = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val allCards = flashcardRepository.getAllCardsByUser(userId)
                    val createdToday = allCards.count { it.createdAt >= todayStart }
                    if (createdToday >= maxNewPerDay) {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = "Bạn đã đạt giới hạn $maxNewPerDay thẻ mới hôm nay. Hãy điều chỉnh trong Cài đặt."
                            )
                        }
                        return@launch
                    }

                    flashcardRepository.createFlashcard(
                        Flashcard(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            deckId = deckId,
                            frontText = state.frontText,
                            backText = state.backText,
                            exampleText = state.exampleText.ifBlank { null },
                            imageUrl = state.imageUrl,
                            audioUrl = state.audioUrl
                        )
                    )
                }

                // Auto-share image to community library BEFORE marking saved
                // (isSaved triggers navigation, which would cancel this coroutine)
                val imageUrl = state.imageUrl
                if (!imageUrl.isNullOrBlank() && state.frontText.isNotBlank()) {
                    // Extract base keyword: strip parenthetical suffixes
                    // e.g. "light (noun)" → "light", "run (verb)" → "run"
                    val rawKeyword = state.frontText.trim()
                    val baseKeyword = rawKeyword.replace(Regex("\\s*\\(.*\\)\\s*$"), "").trim()

                    try {
                        if (imageUrl.startsWith("http")) {
                            android.util.Log.d("SharedImg", "Sharing HTTP image: keyword='$baseKeyword' url=$imageUrl")
                            sharedImageApi.share(ShareImageRequest(
                                keyword = baseKeyword,
                                imageUrl = imageUrl
                            ))
                            // Also share with full keyword if different (for polysemy cards)
                            if (baseKeyword != rawKeyword) {
                                sharedImageApi.share(ShareImageRequest(
                                    keyword = rawKeyword,
                                    imageUrl = imageUrl
                                ))
                            }
                            android.util.Log.d("SharedImg", "Share succeeded")
                        } else {
                            val file = File(imageUrl)
                            if (file.exists()) {
                                val requestBody = file.asRequestBody("image/jpeg".toMediaType())
                                val filePart = okhttp3.MultipartBody.Part.createFormData(
                                    "file", file.name, requestBody
                                )
                                val keywordPart = baseKeyword
                                    .toRequestBody("text/plain".toMediaType())
                                val response = sharedImageApi.uploadImage(filePart, keywordPart)
                                android.util.Log.d("SharedImg", "Upload succeeded for keyword='$baseKeyword'")
                                // Also share with full keyword if different
                                if (baseKeyword != rawKeyword) {
                                    sharedImageApi.share(ShareImageRequest(
                                        keyword = rawKeyword,
                                        imageUrl = response.imageUrl
                                    ))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SharedImg", "Share/upload FAILED: ${e.message}", e)
                    }
                }

                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.localizedMessage) }
            }
        }
    }

    fun dismissAnalysis() {
        _uiState.update { it.copy(analysisResult = null) }
        lastAnalyzedWord = ""
    }

    fun selectSense(sense: com.example.myapplication.domain.repository.WordSenseItem) {
        val key = senseKey(sense)
        val alreadySaved = _uiState.value.savedSenseKeys.contains(key)
        _uiState.update { it.copy(selectedSense = sense, senseSaved = alreadySaved) }
    }

    fun dismissSenseDetail() {
        _uiState.update { it.copy(selectedSense = null) }
    }

    /** Save a polysemy sense as a new flashcard in the current deck */
    fun saveSenseAsCard(sense: com.example.myapplication.domain.repository.WordSenseItem) {
        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch
                val lemma = _uiState.value.analysisResult?.lemma ?: _uiState.value.frontText
                val front = "$lemma (${sense.partOfSpeech})"
                val back = sense.definitionVi ?: sense.definitionEn
                val example = sense.example

                flashcardRepository.createFlashcard(
                    Flashcard(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        deckId = deckId,
                        frontText = front,
                        backText = back,
                        exampleText = example
                    )
                )
                val key = senseKey(sense)
                _uiState.update {
                    it.copy(
                        senseSaved = true,
                        savedSenseKeys = it.savedSenseKeys + key
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Lưu thất bại: ${e.localizedMessage}") }
            }
        }
    }

    private fun senseKey(sense: com.example.myapplication.domain.repository.WordSenseItem): String {
        return "${sense.partOfSpeech}|${sense.definitionEn}"
    }

    /** Fetch community image suggestions for the given keyword */
    private suspend fun fetchSuggestedImages(keyword: String) {
        if (keyword.length < 2) {
            _uiState.update { it.copy(suggestedImages = emptyList(), isLoadingSuggestions = false) }
            return
        }
        _uiState.update { it.copy(isLoadingSuggestions = true) }
        try {
            val results = sharedImageApi.search(keyword, 10)
            android.util.Log.d("SharedImg", "Search '$keyword' → ${results.size} results")
            _uiState.update { it.copy(suggestedImages = results, isLoadingSuggestions = false) }
        } catch (e: Exception) {
            android.util.Log.e("SharedImg", "Search FAILED for '$keyword': ${e.message}")
            _uiState.update { it.copy(suggestedImages = emptyList(), isLoadingSuggestions = false) }
        }
    }

    /** User picks a suggested community image */
    fun selectSuggestedImage(image: SharedImageDto) {
        _uiState.update { it.copy(imageUrl = image.imageUrl) }
        // Increment usage count in background
        viewModelScope.launch {
            try { sharedImageApi.incrementUsage(image.id) } catch (_: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}
