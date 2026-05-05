package com.example.myapplication.presentation.flashcardeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun FlashcardEditorScreen(
    modifier: Modifier = Modifier,
    viewModel: FlashcardEditorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme

    // Local text state — completely decoupled from ViewModel StateFlow
    // to prevent Vietnamese IME composing issues
    var localFront by remember { mutableStateOf(uiState.frontText) }
    var localBack by remember { mutableStateOf(uiState.backText) }
    var localExample by remember { mutableStateOf(uiState.exampleText) }
    var localIpa by remember { mutableStateOf(uiState.pronunciationIpa) }

    // Sync FROM ViewModel ONLY when it changes from external source
    // (e.g. AI-generated example, loaded card data)
    // Use a flag to avoid feedback loops
    val lastVmFront = remember { mutableStateOf(uiState.frontText) }
    val lastVmBack = remember { mutableStateOf(uiState.backText) }
    val lastVmExample = remember { mutableStateOf(uiState.exampleText) }
    val lastVmIpa = remember { mutableStateOf(uiState.pronunciationIpa) }

    if (uiState.frontText != lastVmFront.value) {
        lastVmFront.value = uiState.frontText
        localFront = uiState.frontText
    }
    if (uiState.backText != lastVmBack.value) {
        lastVmBack.value = uiState.backText
        localBack = uiState.backText
    }
    if (uiState.exampleText != lastVmExample.value) {
        lastVmExample.value = uiState.exampleText
        localExample = uiState.exampleText
    }
    if (uiState.pronunciationIpa != lastVmIpa.value) {
        lastVmIpa.value = uiState.pronunciationIpa
        localIpa = uiState.pronunciationIpa
    }

    // Debounced sync TO ViewModel (won't interrupt IME)
    LaunchedEffect(Unit) {
        snapshotFlow { localFront }
            .debounce(500L)
            .collect { viewModel.updateFront(it) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { localBack }
            .debounce(500L)
            .collect { viewModel.updateBack(it) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { localExample }
            .debounce(500L)
            .collect { viewModel.updateExample(it) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { localIpa }
            .debounce(500L)
            .collect { viewModel.updateIpa(it) }
    }

    // Helper: sync all local text to ViewModel immediately (before save)
    fun syncToViewModel() {
        viewModel.updateFront(localFront)
        viewModel.updateBack(localBack)
        viewModel.updateExample(localExample)
        viewModel.updateIpa(localIpa)
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImagePicked(it) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        modifier = modifier,
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Sửa thẻ" else "Tạo thẻ mới",
                        color = cs.onSurface, fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = cs.onSurface)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { syncToViewModel(); viewModel.save() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = cs.primary)
                        } else {
                            Icon(Icons.Default.Check, "Lưu", tint = cs.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(8.dp))

                // Error
                if (!uiState.error.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cs.error.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            "❌ ${uiState.error}",
                            color = cs.error,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // ── Front text + TTS ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Mặt trước (Câu hỏi / Từ vựng)",
                        color = cs.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.isTtsReady && uiState.frontText.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.speakFront() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                "Phát âm",
                                tint = cs.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = localFront,
                    onValueChange = { localFront = it },
                    placeholder = { Text("Ví dụ: Apple", color = cs.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = editorFieldColors()
                )

                // ── Polysemy analysis button ──
                AnimatedVisibility(
                    visible = uiState.analysisResult == null && !uiState.isAnalyzing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Button(
                        onClick = {
                            viewModel.updateFront(localFront)
                            viewModel.analyzeWord()
                        },
                        enabled = localFront.trim().length >= 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.secondaryContainer,
                            contentColor = cs.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Phân tích từ đa nghĩa",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ── Analyzing indicator ──
                AnimatedVisibility(
                    visible = uiState.isAnalyzing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(14.dp),
                            color = cs.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Đang phân tích từ đa nghĩa...",
                            color = cs.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                // ── Analysis results ──
                AnimatedVisibility(
                    visible = uiState.analysisResult != null && !uiState.isAnalyzing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.analysisResult?.let { result ->
                        Column {
                            InlineAnalysisResults(
                                result = result,
                                onSenseClick = { viewModel.selectSense(it) }
                            )
                            // Dismiss button
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                androidx.compose.material3.TextButton(
                                    onClick = { viewModel.dismissAnalysis() }
                                ) {
                                    Text("Đóng", fontSize = 12.sp, color = cs.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Back text + TTS ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Mặt sau (Đáp án / Nghĩa)",
                        color = cs.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.isTtsReady && uiState.backText.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.speakBack() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                "Phát âm",
                                tint = cs.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = localBack,
                    onValueChange = { localBack = it },
                    placeholder = { Text("Ví dụ: Quả táo", color = cs.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = editorFieldColors()
                )

                Spacer(Modifier.height(20.dp))

                // ── IPA Pronunciation + AI generate ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Phiên âm IPA (tùy chọn)",
                        color = cs.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.isGeneratingIpa) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = cs.primary, strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { syncToViewModel(); viewModel.generateIpaWithAi() },
                            modifier = Modifier.size(36.dp),
                            enabled = localFront.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                "AI tạo phiên âm IPA",
                                tint = if (localFront.isNotBlank())
                                    cs.primary else cs.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = localIpa,
                    onValueChange = { localIpa = it },
                    placeholder = { Text("Ví dụ: /ˈæp.əl/", color = cs.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = editorFieldColors()
                )

                Spacer(Modifier.height(20.dp))

                // ── Example + AI generate ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ví dụ (tùy chọn)",
                        color = cs.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.isGeneratingExample) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = cs.primary, strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { syncToViewModel(); viewModel.generateExampleWithAi() },
                            modifier = Modifier.size(36.dp),
                            enabled = localFront.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                "AI tạo ví dụ",
                                tint = if (localFront.isNotBlank())
                                    cs.primary else cs.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = localExample,
                    onValueChange = { localExample = it },
                    placeholder = { Text("Ví dụ: I eat an apple every day.", color = cs.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = editorFieldColors()
                )

                Spacer(Modifier.height(20.dp))

                // ── Image section ──
                Text(
                    "Hình ảnh minh họa (tùy chọn)",
                    color = cs.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))

                if (uiState.imageUrl != null) {
                    // Show image preview
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val imageModel = if (uiState.imageUrl!!.startsWith("http")) {
                                uiState.imageUrl
                            } else {
                                File(uiState.imageUrl!!)
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Ảnh thẻ",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 10f)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        // Remove button
                        IconButton(
                            onClick = { viewModel.removeImage() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Xóa ảnh",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Image placeholder with pick + AI buttons
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            cs.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Image,
                                null,
                                tint = cs.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Nhấn để chọn ảnh từ thư viện",
                                color = cs.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Image action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pick from gallery
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.surfaceContainer)
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            null,
                            tint = cs.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Chọn ảnh", color = cs.onSurface, fontSize = 13.sp)
                    }
                }

                // ── Community Image Suggestions ──
                if (uiState.imageUrl == null && uiState.suggestedImages.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "\uD83D\uDCA1 \u1EA2nh t\u1EEB c\u1ED9ng \u0111\u1ED3ng (${uiState.suggestedImages.size})",
                            color = cs.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.isLoadingSuggestions) {
                            CircularProgressIndicator(
                                Modifier.size(14.dp),
                                color = cs.primary,
                                strokeWidth = 1.5.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.suggestedImages.size) { index ->
                            val img = uiState.suggestedImages[index]
                            Card(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable { viewModel.selectSuggestedImage(img) },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, cs.outlineVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(img.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Community image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    Text(
                        "Ch\u1EA1m \u0111\u1EC3 ch\u1ECDn \u1EA3nh",
                        color = cs.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                // ── Save button ──
                Button(
                    onClick = { syncToViewModel(); viewModel.save() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            if (uiState.isEditMode) "CẬP NHẬT THẺ" else "TẠO THẺ",
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ── Sense Detail Dialog ──
    uiState.selectedSense?.let { sense ->
        val lemma = uiState.analysisResult?.lemma ?: uiState.frontText
        AlertDialog(
            onDismissRequest = { viewModel.dismissSenseDetail() },
            containerColor = cs.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(cs.primary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            posLabel(sense.partOfSpeech),
                            color = cs.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        lemma,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column {
                    // Front text
                    Text("Mặt trước", fontSize = 11.sp, color = cs.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text(
                        "$lemma (${sense.partOfSpeech})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )

                    Spacer(Modifier.height(12.dp))

                    // Back text
                    Text("Mặt sau", fontSize = 11.sp, color = cs.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text(
                        sense.definitionVi ?: sense.definitionEn,
                        fontSize = 16.sp,
                        color = cs.onSurface
                    )
                    if (!sense.definitionVi.isNullOrBlank()) {
                        Text(
                            sense.definitionEn,
                            fontSize = 13.sp,
                            color = cs.onSurfaceVariant
                        )
                    }

                    // Example
                    if (!sense.example.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Ví dụ", fontSize = 11.sp, color = cs.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Text(
                            sense.example!!,
                            fontSize = 14.sp,
                            color = cs.onSurface,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            },
            confirmButton = {
                if (uiState.senseSaved) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Đã lưu!", color = cs.primary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.saveSenseAsCard(sense) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                    ) {
                        Text("Lưu thành thẻ", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.dismissSenseDetail() }
                ) {
                    Text("Đóng", color = cs.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    cursorColor = MaterialTheme.colorScheme.primary
)

/**
 * Compact inline display of polysemy analysis results.
 * Shows right below the front text field when auto-analysis completes.
 */
@Composable
private fun InlineAnalysisResults(
    result: com.example.myapplication.domain.repository.WordAnalysisResult,
    onSenseClick: (com.example.myapplication.domain.repository.WordSenseItem) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                Icons.Default.MenuBook,
                null,
                tint = cs.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Từ: \"${result.lemma}\" (${posLabel(result.detectedPOS)})  •  Nhấn để lưu",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = cs.primary
            )
        }

        // All senses — compact chips
        val allSenses = buildList {
            add(result.mainSense to true)
            result.relatedVariants.forEach { add(it to false) }
            result.otherMeanings.forEach { add(it to false) }
        }

        allSenses.forEach { (sense, isMain) ->
            InlineSenseChip(sense = sense, isMain = isMain, onClick = { onSenseClick(sense) })
            Spacer(Modifier.height(4.dp))
        }

        // Word variants — single row of tags
        if (result.wordVariants.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Biến thể:",
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline()
                )
                result.wordVariants.take(5).forEach { v ->
                    Box(
                        modifier = Modifier
                            .background(
                                cs.surfaceContainer,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            v.variant,
                            fontSize = 11.sp,
                            color = cs.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineSenseChip(
    sense: com.example.myapplication.domain.repository.WordSenseItem,
    isMain: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isMain) cs.primaryContainer.copy(alpha = 0.25f)
                else cs.surfaceContainer
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // POS tag
        Box(
            modifier = Modifier
                .background(
                    if (isMain) cs.primary else cs.secondary.copy(alpha = 0.7f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                posLabel(sense.partOfSpeech).take(4),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onPrimary
            )
        }

        Spacer(Modifier.width(8.dp))

        // Definition
        Column(modifier = Modifier.weight(1f)) {
            Text(
                sense.definitionEn,
                fontSize = 12.sp,
                color = cs.onSurface,
                fontWeight = if (isMain) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (!sense.definitionVi.isNullOrBlank()) {
                Text(
                    sense.definitionVi!!,
                    fontSize = 11.sp,
                    color = cs.primary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        // Main indicator
        if (isMain) {
            Spacer(Modifier.width(4.dp))
            Text("★", fontSize = 14.sp, color = cs.primary)
        }
    }
}

private fun posLabel(pos: String): String = when (pos.lowercase()) {
    "noun" -> "Danh từ"
    "verb" -> "Động từ"
    "adjective", "adj" -> "Tính từ"
    "adverb", "adv" -> "Trạng từ"
    "preposition" -> "Giới từ"
    "pronoun" -> "Đại từ"
    "conjunction" -> "Liên từ"
    else -> pos
}
