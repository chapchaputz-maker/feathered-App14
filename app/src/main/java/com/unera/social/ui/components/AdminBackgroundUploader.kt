package com.unera.social.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.data.BackgroundsApiService
import com.unera.social.data.MediaUploadHelper
import com.unera.social.data.RemotePostBackground
import com.unera.social.model.User
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily
import kotlinx.coroutines.launch

private data class BackgroundCategoryPreset(
    val value: String,
    val label: String,
    val emoji: String
)

private val ADMIN_BG_CATEGORY_PRESETS = listOf(
    BackgroundCategoryPreset("nature", "Nature", "🌿"),
    BackgroundCategoryPreset("love", "Love", "❤️"),
    BackgroundCategoryPreset("flowers", "Flowers", "🌸"),
    BackgroundCategoryPreset("sad", "Sad", "🌧️"),
    BackgroundCategoryPreset("adventure", "Adventure", "🧭"),
    BackgroundCategoryPreset("environment", "Environment", "🌍"),
    BackgroundCategoryPreset("romantic", "Romantic", "💕"),
    BackgroundCategoryPreset("magical", "Magical", "✨"),
    BackgroundCategoryPreset("animals", "Animals", "🦋"),
    BackgroundCategoryPreset("sunset", "Sunset", "🌅"),
    BackgroundCategoryPreset("motivation", "Motivation", "🔥"),
    BackgroundCategoryPreset("celebration", "Celebration", "🎉"),
    BackgroundCategoryPreset("fantasy", "Fantasy", "🪄"),
    BackgroundCategoryPreset("peaceful", "Peaceful", "🕊️"),
    BackgroundCategoryPreset("3d_art", "3D Art", "🎨")
)

/**
 * Admin-only Background Image Uploader card displayed exclusively on the platform admin's own profile.
 * Opens the Android Photo Picker, lets the admin select from 15 categories, compresses to high-quality
 * feed.webp + thumb.webp, uploads both via POST /api/upload, and registers the record via POST /api/admin/backgrounds
 * so it appears inside the Reality category on Backgrounds for text.
 */
@Composable
fun AdminBackgroundStudioCard(
    currentUser: User,
    onUploadSuccess: (RemotePostBackground) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Hard safety guard: only render for platform admin viewing their own profile
    if (!currentUser.role.equals("admin", ignoreCase = true)) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val remoteBackgrounds by BackgroundsApiService.backgrounds.collectAsState()
    val isLoadingBgs by BackgroundsApiService.isLoading.collectAsState()

    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        BackgroundsApiService.fetchBackgrounds(context, forceRefresh = true)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImageUri = uri
        }
    }

    Surface(
        color = AppColors.bgCardElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppColors.primaryBlue.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_background_studio_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(AppColors.primaryBlue, Color(0xFF38BDF8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Background Uploader",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = AppColors.textPrimary
                        )
                        Surface(
                            color = AppColors.bgBlueTint,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                color = AppColors.primaryBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Add images to Reality text post backgrounds (${remoteBackgrounds.size} live)",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = AppColors.textSecondary
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            BackgroundsApiService.fetchBackgrounds(context, forceRefresh = true)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("admin_bg_refresh_btn")
                ) {
                    if (isLoadingBgs) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.primaryBlue
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh backgrounds",
                            tint = AppColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Recent uploaded thumbnails strip (data saver: strictly thumbnail_url)
            if (remoteBackgrounds.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_bg_recent_strip")
                ) {
                    items(remoteBackgrounds.take(12), key = { "admin_bg_${it.id}" }) { bg ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(62.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, AppColors.borderStrong, RoundedCornerShape(10.dp))
                            ) {
                                RemoteImageBackgroundLayer(
                                    background = bg,
                                    modifier = Modifier.matchParentSize(),
                                    isVisible = true,
                                    useThumbnailOnly = true
                                )
                                if (bg.isAnimated) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF38BDF8))
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = bg.normalizedSubCategory(),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.sp,
                                color = AppColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_upload_background_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upload",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Category + Quality Compression Upload Dialog opened right after gallery selection
    val selectedUri = pendingImageUri
    if (selectedUri != null) {
        AdminBackgroundUploadDialog(
            imageUri = selectedUri,
            adminUserId = currentUser.id,
            onPickDifferentImage = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { pendingImageUri = null },
            onUploaded = { created ->
                pendingImageUri = null
                onUploadSuccess(created)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminBackgroundUploadDialog(
    imageUri: Uri,
    adminUserId: Long,
    onPickDifferentImage: () -> Unit,
    onDismiss: () -> Unit,
    onUploaded: (RemotePostBackground) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val derivedDefaultName = remember(imageUri) {
        resolveCleanImageName(context, imageUri)
    }
    val isAnimatedSource = remember(imageUri) {
        MediaUploadHelper.detectAnimatedImage(context, imageUri).first
    }

    var nameInput by remember(imageUri) { mutableStateOf(derivedDefaultName) }
    var categoryInput by remember { mutableStateOf("nature") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = !isUploading) {
        onDismiss()
    }

    Dialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.bgScreen)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("admin_background_upload_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(AppColors.headerBg)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (!isUploading) onDismiss() },
                        enabled = !isUploading,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.headerIconTint
                        )
                    }
                    Text(
                        text = "Upload Reality Background",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = AppColors.headerText,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { if (!isUploading) onPickDifferentImage() },
                        enabled = !isUploading,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = "Change",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (AppColors.isLight) Color.White else AppColors.primaryBlue
                        )
                    }
                }

                HorizontalDivider(color = AppColors.headerDivider, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Live Composer Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, AppColors.border, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected background preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.24f))
                        )
                        Text(
                            text = "Your text post will appear crisp and clear inside Reality.",
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                lineHeight = 26.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.8f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 8f
                                )
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (isAnimatedSource) "✨ Animated WebP/GIF" else "🖼️ Reality WebP",
                                color = Color.White,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Quality compression info pill
                    Surface(
                        color = AppColors.bgBlueTint,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AppColors.primaryBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = AppColors.primaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isAnimatedSource) {
                                    "Preserves animation for feed & creates a fast 420px WebP thumbnail."
                                } else {
                                    "High-quality WebP dual compression: feed.webp (1440px, 92%) + thumb.webp (420px, 86%)."
                                },
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = AppColors.textPrimary
                            )
                        }
                    }

                    // Background Name Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Background Name",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = AppColors.textPrimary
                        )
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            enabled = !isUploading,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    "e.g. Golden Rose Garden",
                                    fontFamily = InterFontFamily,
                                    color = AppColors.textMuted
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = AppColors.textPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppColors.bgCard,
                                unfocusedContainerColor = AppColors.bgCard,
                                focusedBorderColor = AppColors.primaryBlue,
                                unfocusedBorderColor = AppColors.borderStrong
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_bg_name_input")
                        )
                    }

                    // Category Input + 15 Selectable Presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Category (${ADMIN_BG_CATEGORY_PRESETS.size} presets)",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = AppColors.textPrimary
                            )
                            Text(
                                text = "Appears in Reality",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = AppColors.primaryBlue
                            )
                        }

                        OutlinedTextField(
                            value = categoryInput,
                            onValueChange = { categoryInput = it },
                            enabled = !isUploading,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    "Select below or enter custom category",
                                    fontFamily = InterFontFamily,
                                    color = AppColors.textMuted
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.textPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppColors.bgCard,
                                unfocusedContainerColor = AppColors.bgCard,
                                focusedBorderColor = AppColors.primaryBlue,
                                unfocusedBorderColor = AppColors.borderStrong
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_bg_category_input")
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ADMIN_BG_CATEGORY_PRESETS.forEach { preset ->
                                val isSelected = categoryInput.trim().equals(preset.value, ignoreCase = true) ||
                                    categoryInput.trim().equals(preset.label, ignoreCase = true)
                                Surface(
                                    onClick = { if (!isUploading) categoryInput = preset.value },
                                    color = if (isSelected) AppColors.primaryBlue else AppColors.bgCard,
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) AppColors.primaryBlue else AppColors.borderStrong
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .testTag("admin_bg_cat_${preset.value}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(preset.emoji, fontSize = 13.sp)
                                        Text(
                                            text = preset.label,
                                            fontFamily = InterFontFamily,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else AppColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Upload progress indicator
                    AnimatedVisibility(visible = isUploading) {
                        Surface(
                            color = AppColors.bgCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AppColors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uploadStage.ifBlank { "Uploading..." },
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = AppColors.textPrimary
                                    )
                                    Text(
                                        text = "${(uploadProgress * 100).toInt()}%",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = AppColors.primaryBlue
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { uploadProgress.coerceIn(0f, 1f) },
                                    color = AppColors.primaryBlue,
                                    trackColor = AppColors.bgChip,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }

                    // Error banner
                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = AppColors.dangerBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AppColors.dangerText.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = AppColors.dangerText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = AppColors.dangerText,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Bottom CTA
                Surface(
                    color = AppColors.bgCard,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = AppColors.border, thickness = 1.dp)
                        Button(
                            onClick = {
                                if (isUploading) return@Button
                                val finalCategory = categoryInput.trim().ifBlank { "nature" }
                                val finalName = nameInput.trim().ifBlank { "Reality Scene" }
                                errorMessage = null
                                isUploading = true
                                uploadProgress = 0.08f
                                uploadStage = "Preparing image..."

                                coroutineScope.launch {
                                    val bundle = MediaUploadHelper.processAndUploadBackgroundImages(
                                        context = context,
                                        uri = imageUri,
                                        userId = adminUserId,
                                        onProgress = { stage, pct ->
                                            uploadStage = stage
                                            uploadProgress = pct
                                        }
                                    )
                                    if (bundle == null) {
                                        isUploading = false
                                        errorMessage = "Failed to process or upload feed.webp / thumb.webp. Please check your connection and try again."
                                        return@launch
                                    }

                                    uploadStage = "Saving background to database..."
                                    uploadProgress = 0.90f

                                    val saveResult = BackgroundsApiService.createAdminBackground(
                                        context = context,
                                        userId = adminUserId,
                                        name = finalName,
                                        url = bundle.feedUrl,
                                        thumbnailUrl = bundle.thumbnailUrl,
                                        category = finalCategory,
                                        isAnimated = bundle.isAnimated,
                                        width = bundle.width,
                                        height = bundle.height,
                                        dominantColor = bundle.dominantColor,
                                        textColor = bundle.recommendedTextColor,
                                        overlayColor = bundle.recommendedOverlayColor,
                                        overlayOpacity = bundle.recommendedOverlayOpacity,
                                        format = bundle.format,
                                        mimeType = bundle.mimeType,
                                        fileSize = bundle.fileSize,
                                        thumbWidth = bundle.thumbWidth,
                                        thumbHeight = bundle.thumbHeight,
                                        thumbFileSize = bundle.thumbFileSize
                                    )

                                    isUploading = false
                                    saveResult.onSuccess { created ->
                                        uploadProgress = 1f
                                        onUploaded(created)
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Failed to save background record."
                                    }
                                }
                            },
                            enabled = !isUploading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.primaryBlue,
                                contentColor = Color.White,
                                disabledContainerColor = AppColors.bgChip
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(48.dp)
                                .testTag("admin_bg_submit_btn")
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Uploading...",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Upload to Reality Backgrounds",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveCleanImageName(context: android.content.Context, uri: Uri): String {
    return try {
        var displayName: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) displayName = cursor.getString(idx)
            }
        }
        val base = displayName?.substringBeforeLast(".")
            ?.replace(Regex("[_-]+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.all { ch -> ch.isDigit() } }
        base?.split(Regex("\\s+"))
            ?.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
            ?.take(40)
            ?: "Reality Background"
    } catch (_: Exception) {
        "Reality Background"
    }
}
