package com.unera.social.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.unera.social.data.BackgroundsApiService
import com.unera.social.data.RemotePostBackground
import com.unera.social.data.StoriesApiService
import com.unera.social.model.Song
import com.unera.social.model.User
import com.unera.social.ui.components.ALL_TEXT_POST_BACKGROUNDS
import com.unera.social.ui.components.AnimatedBgCategory
import com.unera.social.ui.components.AnimatedPostBackground
import com.unera.social.ui.components.RemoteImageBackgroundLayer
import com.unera.social.ui.components.TextPostBackgroundTemplate
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class StoryGradient(
    val css: String,
    val colors: List<Color>
)

val STORY_COLORS = listOf(
    StoryGradient("linear-gradient(45deg, #1877F2, #0055FF)", listOf(Color(0xFF1877F2), Color(0xFF0055FF))),
    StoryGradient("linear-gradient(45deg, #F3425F, #E41E3F)", listOf(Color(0xFFF3425F), Color(0xFFE41E3F))),
    StoryGradient("linear-gradient(45deg, #45BD62, #31A24C)", listOf(Color(0xFF45BD62), Color(0xFF31A24C))),
    StoryGradient("linear-gradient(45deg, #F7B928, #E3A300)", listOf(Color(0xFFF7B928), Color(0xFFE3A300))),
    StoryGradient("linear-gradient(45deg, #A033FF, #7B1FA2)", listOf(Color(0xFFA033FF), Color(0xFF7B1FA2))),
    StoryGradient("linear-gradient(45deg, #FF7E5F, #FEB47B)", listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B))),
    StoryGradient("linear-gradient(45deg, #00C6FF, #0072FF)", listOf(Color(0xFF00C6FF), Color(0xFF0072FF))),
    StoryGradient("linear-gradient(45deg, #2193b0, #6dd5ed)", listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))),
    StoryGradient("linear-gradient(45deg, #ee9ca7, #ffdde1)", listOf(Color(0xFFEE9CA7), Color(0xFFFFDDE1))),
    StoryGradient("linear-gradient(45deg, #42275a, #734b6d)", listOf(Color(0xFF42275A), Color(0xFF734B6D))),
    StoryGradient("linear-gradient(45deg, #BDC3C7, #2C3E50)", listOf(Color(0xFFBDC3C7), Color(0xFF2C3E50))),
    StoryGradient("linear-gradient(45deg, #000000, #434343)", listOf(Color(0xFF000000), Color(0xFF434343)))
)

data class MediaPick(
    val uri: Uri
)

data class SelectedMusicInfo(
    val title: String,
    val artist: String,
    val url: String,
    val coverUrl: String? = null
)

/**
 * CreateStoryDialog
 * Native Android implementation matching web Story.tsx (CreateStoryModal):
 * - Discard / Create Story / Share top bar
 * - Text mode with 12 STORY_COLORS gradient backgrounds & centered text input
 * - Media mode with empty placeholder & multi-image picker (videos disabled)
 * - Media preview with uncropped true-to-aspect-ratio scaling and ambient backdrop
 * - Floating music pill with cancel button
 * - Bottom controls: Text mode color carousel, mode pill switcher (Text/Media), Camera icon, Plus media icon, Music icon
 * - Live Music Picker modal fetching /api/songs live, with audio file upload, audio preview, and Add button (no fake songs)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmitStory: (
        mediaUri: Uri?,
        isVideo: Boolean,
        type: String,
        textContent: String,
        backgroundStyle: String,
        musicTitle: String?,
        musicUrl: String?
    ) -> Unit,
    isSubmitting: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mode by remember { mutableStateOf("media") } // "media" or "text"
    var textContent by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var activeCategoryKey by remember { mutableStateOf("STORY") } // "STORY" or AnimatedBgCategory.name
    var selectedTemplate by remember { mutableStateOf<TextPostBackgroundTemplate?>(null) }
    var selectedRemoteBackground by remember { mutableStateOf<RemotePostBackground?>(null) }
    var activeRealitySubCategory by remember { mutableStateOf("All") }
    var showTextStyleControls by remember { mutableStateOf(false) }

    // Text formatting controls (mirrors CreatePostDialog)
    var textAlignment by remember { mutableStateOf(TextAlign.Center) }
    var textPosition by remember { mutableStateOf("center") }
    var textTextColor by remember { mutableStateOf(Color.White) }
    var textFontSizeSp by remember { mutableIntStateOf(28) }
    var textIsBold by remember { mutableStateOf(true) }
    var textFontStyle by remember { mutableStateOf("sans") }

    var picks by remember { mutableStateOf<List<MediaPick>>(emptyList()) }
    var activePickIndex by remember { mutableIntStateOf(0) }
    var selectedMusic by remember { mutableStateOf<SelectedMusicInfo?>(null) }
    var showMusicPicker by remember { mutableStateOf(false) }
    var localSubmitting by remember { mutableStateOf(false) }

    val remoteBackgrounds by BackgroundsApiService.backgrounds.collectAsState()
    val isLoadingRemoteBgs by BackgroundsApiService.isLoading.collectAsState()
    val remoteBgError by BackgroundsApiService.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        BackgroundsApiService.ensureLoaded(context)
    }

    val activeGradient = STORY_COLORS[selectedColorIndex]

    // Launcher for taking a quick photo with camera
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "story_cam_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                val uri = Uri.fromFile(file)
                picks = picks + MediaPick(uri = uri)
                activePickIndex = picks.size - 1
                mode = "media"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Launcher for selecting multiple photos from system gallery (ImageOnly)
    val pickMultipleMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val newPicks = uris.mapNotNull { uri ->
                val mime = try { context.contentResolver.getType(uri) ?: "" } catch (_: Exception) { "" }
                val isVid = mime.startsWith("video") || uri.toString().contains(".mp4", ignoreCase = true)
                if (isVid) {
                    Toast.makeText(context, "Story videos are disabled. Only photos are supported.", Toast.LENGTH_SHORT).show()
                    null
                } else {
                    MediaPick(uri = uri)
                }
            }
            if (newPicks.isNotEmpty()) {
                picks = (picks + newPicks).take(30)
                mode = "media"
            }
        }
    }

    val busy = isSubmitting || localSubmitting
    val canShare = (mode == "text" && textContent.trim().isNotBlank()) || (mode == "media" && picks.isNotEmpty())

    val buildFinalBackgroundCss: () -> String = {
        val alignStr = when (textAlignment) {
            TextAlign.Start -> "left"
            TextAlign.End -> "right"
            else -> "center"
        }
        val colorHex = String.format("#%06X", (0xFFFFFF and textTextColor.toArgb()))
        val weightStr = if (textIsBold) "bold" else "normal"
        val isDefaultStyling = textAlignment == TextAlign.Center &&
            textPosition == "center" &&
            textTextColor == Color.White &&
            textFontSizeSp == 28 &&
            textIsBold &&
            textFontStyle == "sans"

        val remote = selectedRemoteBackground
        val tpl = selectedTemplate
        when {
            remote != null ->
                "reality:${remote.id}|bgtype:${remote.backgroundTypeValue}|align:$alignStr|pos:$textPosition|color:$colorHex|size:$textFontSizeSp|font:$textFontStyle|weight:$weightStr"
            tpl != null && tpl.styleString.isNotBlank() ->
                "${tpl.styleString}|align:$alignStr|pos:$textPosition|color:$colorHex|size:$textFontSizeSp|font:$textFontStyle|weight:$weightStr"
            isDefaultStyling -> activeGradient.css
            else ->
                "${activeGradient.css}|align:$alignStr|pos:$textPosition|color:$colorHex|size:$textFontSizeSp|font:$textFontStyle|weight:$weightStr"
        }
    }

    val handleShare = {
        if (canShare && !busy) {
            localSubmitting = true
            val backgroundCss = if (mode == "text") buildFinalBackgroundCss() else activeGradient.css
            val musicTitleStr = selectedMusic?.let { "${it.title} - ${it.artist}" }
            val musicUrlStr = selectedMusic?.url

            if (mode == "text") {
                onSubmitStory(
                    null,
                    false,
                    "text",
                    textContent.trim(),
                    backgroundCss,
                    musicTitleStr,
                    musicUrlStr
                )
            } else {
                val active = picks.getOrNull(activePickIndex) ?: picks.first()
                onSubmitStory(
                    active.uri,
                    false,
                    "image",
                    textContent.trim(),
                    backgroundCss,
                    musicTitleStr,
                    musicUrlStr
                )
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !busy,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Header Bar (pure black cinema header)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.Black)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { if (!busy) onDismiss() },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1A1A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("story_discard_btn")
                    ) {
                        Text(
                            text = "Discard",
                            fontFamily = InterFontFamily,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "Create Story",
                        fontFamily = InterFontFamily,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )

                    Button(
                        onClick = { handleShare() },
                        enabled = canShare && !busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            disabledContainerColor = Color(0xFF262626),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("story_share_btn")
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Share",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Main Preview Canvas (flexes cleanly above bottom dock)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                ) {
                    if (mode == "text") {
                        val currentRemote = selectedRemoteBackground
                        val currentTpl = selectedTemplate
                        val editorAlignment = when (textPosition) {
                            "top" -> when (textAlignment) {
                                TextAlign.Start -> Alignment.TopStart
                                TextAlign.End -> Alignment.TopEnd
                                else -> Alignment.TopCenter
                            }
                            "bottom" -> when (textAlignment) {
                                TextAlign.Start -> Alignment.BottomStart
                                TextAlign.End -> Alignment.BottomEnd
                                else -> Alignment.BottomCenter
                            }
                            else -> when (textAlignment) {
                                TextAlign.Start -> Alignment.CenterStart
                                TextAlign.End -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                        }
                        val isDarkEditorText = textTextColor.luminance() < 0.45f
                        val editorTextShadow = if (isDarkEditorText) {
                            Shadow(color = Color.White.copy(alpha = 0.55f), offset = Offset(1f, 1f), blurRadius = 6f)
                        } else {
                            Shadow(color = Color.Black.copy(alpha = 0.80f), offset = Offset(2f, 2f), blurRadius = 9f)
                        }
                        val editorFontWeight = if (textIsBold) FontWeight.Bold else FontWeight.Normal
                        val editorFontFamily = when (textFontStyle) {
                            "serif" -> FontFamily.Serif
                            "monospace" -> FontFamily.Monospace
                            "cursive" -> FontFamily.Cursive
                            else -> InterFontFamily
                        }
                        val editorFontStyle = if (textFontStyle == "italic") FontStyle.Italic else FontStyle.Normal

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    when {
                                        currentRemote != null -> Modifier
                                        currentTpl != null && currentTpl.isAnimated -> Modifier
                                        currentTpl != null && currentTpl.baseColors.isNotEmpty() ->
                                            Modifier.background(Brush.linearGradient(currentTpl.baseColors))
                                        else -> Modifier.background(Brush.linearGradient(activeGradient.colors))
                                    }
                                ),
                            contentAlignment = editorAlignment
                        ) {
                            if (currentRemote != null) {
                                RemoteImageBackgroundLayer(
                                    background = currentRemote,
                                    modifier = Modifier.matchParentSize(),
                                    isVisible = true
                                )
                                val scrimAlpha = currentRemote.effectiveOverlayAlpha(textTextColor)
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(currentRemote.parsedOverlayColor().copy(alpha = scrimAlpha))
                                )
                                if (textPosition == "top" || textPosition == "bottom") {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                if (textPosition == "top") {
                                                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent))
                                                } else {
                                                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)))
                                                }
                                            )
                                    )
                                }
                            } else if (currentTpl != null && currentTpl.isAnimated) {
                                AnimatedPostBackground(
                                    templateId = currentTpl.id,
                                    modifier = Modifier.matchParentSize(),
                                    isVisible = true
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Black.copy(alpha = 0.22f))
                                )
                            }

                            BasicTextField(
                                value = textContent,
                                onValueChange = { textContent = it },
                                textStyle = TextStyle(
                                    fontFamily = editorFontFamily,
                                    fontStyle = editorFontStyle,
                                    color = textTextColor,
                                    fontSize = textFontSizeSp.sp,
                                    lineHeight = (textFontSizeSp * 1.4f).sp,
                                    fontWeight = editorFontWeight,
                                    textAlign = textAlignment,
                                    shadow = editorTextShadow
                                ),
                                cursorBrush = SolidColor(textTextColor),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = editorAlignment
                                    ) {
                                        if (textContent.isEmpty()) {
                                            Text(
                                                text = "Start typing...",
                                                fontFamily = editorFontFamily,
                                                fontStyle = editorFontStyle,
                                                color = textTextColor.copy(alpha = 0.55f),
                                                fontSize = textFontSizeSp.sp,
                                                fontWeight = editorFontWeight,
                                                textAlign = textAlignment,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = if (selectedMusic != null) 64.dp else 28.dp)
                                    .wrapContentHeight()
                                    .testTag("story_text_input")
                            )
                        }
                    } else {
                        // Media Mode: Photos only (Videos disabled) on pure black
                        if (picks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                    .clickable {
                                        pickMultipleMediaLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF141414))
                                            .border(1.dp, Color(0xFF262626), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Select Photos",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Select Photos",
                                        fontFamily = InterFontFamily,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Choose high quality photos for your story",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Media Preview with active pick - pure black cinema backdrop (matching Videos)
                            val activePick = picks.getOrNull(activePickIndex) ?: picks.first()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = activePick.uri,
                                    contentDescription = "Story preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Delete active pick button (top-left)
                                IconButton(
                                    onClick = {
                                        val newPicks = picks.toMutableList()
                                        newPicks.removeAt(activePickIndex)
                                        picks = newPicks
                                        if (activePickIndex >= newPicks.size) {
                                            activePickIndex = maxOf(0, newPicks.size - 1)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(16.dp)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete media",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Dot indicators & thumbnail strip if multiple items
                                if (picks.size > 1) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 74.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.Black.copy(alpha = 0.55f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        picks.indices.forEach { idx ->
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (idx == activePickIndex) Color.White else Color.White.copy(alpha = 0.4f)
                                                    )
                                            )
                                        }
                                    }

                                    LazyRow(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(picks.indices.toList()) { idx ->
                                            val p = picks[idx]
                                            val isSelected = idx == activePickIndex
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) Color(0xFF1877F2) else Color.White.copy(alpha = 0.25f),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { activePickIndex = idx }
                                            ) {
                                                AsyncImage(
                                                    model = p.uri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.White.copy(alpha = 0.12f))
                                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        pickMultipleMediaLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add media",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Music Badge (top of canvas)
                    selectedMusic?.let { music ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1877F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = music.title,
                                        fontFamily = InterFontFamily,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = music.artist,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { selectedMusic = null },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove music",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Dock (Pure Black matching Videos)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF1C1C1C))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (mode == "text") {
                        // Collapsible Text Formatting Toolbar (Alignment, Vertical Pos, Bold, Font, Color, Size)
                        AnimatedVisibility(visible = showTextStyleControls) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF121212))
                                    .border(1.dp, Color(0xFF242424), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Row 1: Align, Vertical Pos, Bold, Font Style
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E1E))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { textAlignment = TextAlign.Start },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(if (textAlignment == TextAlign.Start) Color(0xFF1877F2) else Color.Transparent, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Left", tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = { textAlignment = TextAlign.Center },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(if (textAlignment == TextAlign.Center) Color(0xFF1877F2) else Color.Transparent, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center", tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = { textAlignment = TextAlign.End },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(if (textAlignment == TextAlign.End) Color(0xFF1877F2) else Color.Transparent, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.FormatAlignRight, contentDescription = "Align Right", tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E1E))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { textPosition = "top" },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(if (textPosition == "top") Color(0xFF1877F2) else Color.Transparent, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.VerticalAlignTop, contentDescription = "Top", tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = { textPosition = "center" },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(if (textPosition == "center") Color(0xFF1877F2) else Color.Transparent, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.VerticalAlignCenter, contentDescription = "Center", tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = { textPosition = "bottom" },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(if (textPosition == "bottom") Color(0xFF1877F2) else Color.Transparent, RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Bottom", tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (textIsBold) Color(0xFF1877F2) else Color(0xFF1E1E1E))
                                                .clickable { textIsBold = !textIsBold },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("B", fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF1E1E1E))
                                                .clickable {
                                                    textFontStyle = when (textFontStyle) {
                                                        "sans" -> "serif"
                                                        "serif" -> "display"
                                                        "display" -> "italic"
                                                        "italic" -> "cursive"
                                                        else -> "sans"
                                                    }
                                                }
                                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = when (textFontStyle) {
                                                    "serif" -> "Serif"
                                                    "display" -> "Display"
                                                    "italic" -> "Italic"
                                                    "cursive" -> "Script"
                                                    else -> "Sans"
                                                },
                                                fontSize = 11.sp,
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                }

                                // Row 2: Text color swatches + Auto contrast + Font size stepper
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF1E1E1E))
                                                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    textTextColor = selectedRemoteBackground?.resolvedDefaultTextColor()
                                                        ?: selectedTemplate?.defaultTextColor
                                                        ?: Color.White
                                                }
                                                .padding(horizontal = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Auto", fontSize = 10.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, color = Color(0xFF38BDF8))
                                        }

                                        val swatches = listOf(
                                            Color.White,
                                            Color(0xFFFEF08A),
                                            Color(0xFFA5F3FC),
                                            Color(0xFFFECDD3),
                                            Color(0xFFBBF7D0),
                                            Color(0xFF0F172A)
                                        )
                                        swatches.forEach { c ->
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(c)
                                                    .border(
                                                        if (textTextColor == c) 2.dp else 1.dp,
                                                        if (textTextColor == c) Color(0xFF1877F2) else Color.White.copy(alpha = 0.35f),
                                                        CircleShape
                                                    )
                                                    .clickable { textTextColor = c }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E1E))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { if (textFontSizeSp > 18) textFontSizeSp -= 2 },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("A-", fontSize = 11.sp, color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(
                                            text = "${textFontSizeSp}sp",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { if (textFontSizeSp < 36) textFontSizeSp += 2 },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("A+", fontSize = 12.sp, color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Background Category Chips: Preserved Story Gradients + All Create Post Categories
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag("story_bg_categories")
                        ) {
                            item(key = "cat_story") {
                                val isCatSelected = activeCategoryKey == "STORY"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isCatSelected) Color(0xFF1877F2) else Color(0xFF1A1A1A))
                                        .clickable { activeCategoryKey = "STORY" }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("story_bg_category_story")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🌈", fontSize = 12.sp, fontFamily = InterFontFamily)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Gradients",
                                            fontSize = 12.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = if (isCatSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            items(AnimatedBgCategory.values()) { cat ->
                                val isCatSelected = activeCategoryKey == cat.name
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isCatSelected) Color(0xFF1877F2) else Color(0xFF1A1A1A))
                                        .clickable {
                                            activeCategoryKey = cat.name
                                            if (cat == AnimatedBgCategory.REALITY && !isLoadingRemoteBgs) {
                                                coroutineScope.launch {
                                                    BackgroundsApiService.fetchBackgrounds(context, forceRefresh = true)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("story_bg_category_${cat.name.lowercase()}")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(cat.emoji, fontSize = 12.sp, fontFamily = InterFontFamily)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = cat.label,
                                            fontSize = 12.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = if (isCatSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Background Swatches / Thumbnails for Active Category
                        when (activeCategoryKey) {
                            "STORY" -> {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                ) {
                                    items(STORY_COLORS.indices.toList()) { idx ->
                                        val grad = STORY_COLORS[idx]
                                        val isSelected = selectedTemplate == null &&
                                            selectedRemoteBackground == null &&
                                            selectedColorIndex == idx
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(grad.colors))
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    selectedTemplate = null
                                                    selectedRemoteBackground = null
                                                    selectedColorIndex = idx
                                                }
                                        )
                                    }
                                }
                            }

                            AnimatedBgCategory.REALITY.name -> {
                                val realitySubCats = remember(remoteBackgrounds) {
                                    val cats = remoteBackgrounds.map { it.normalizedSubCategory() }.distinct().filter { it != "All" }
                                    if (cats.isNotEmpty()) listOf("All") + cats else listOf("All")
                                }

                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item(key = "story_reality_sync") {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF141414))
                                                .border(1.dp, Color(0xFF2B2B2B), RoundedCornerShape(12.dp))
                                                .clickable(enabled = !isLoadingRemoteBgs) {
                                                    coroutineScope.launch {
                                                        BackgroundsApiService.fetchBackgrounds(context, forceRefresh = true)
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (isLoadingRemoteBgs) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(11.dp),
                                                        strokeWidth = 1.5.dp,
                                                        color = Color(0xFF38BDF8)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Refresh Reality backgrounds",
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "Sync",
                                                    fontSize = 11.sp,
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF38BDF8)
                                                )
                                            }
                                        }
                                    }

                                    items(realitySubCats) { sub ->
                                        val isSubSelected = activeRealitySubCategory == sub
                                        val catCount = if (sub == "All") remoteBackgrounds.size else remoteBackgrounds.count { it.normalizedSubCategory().equals(sub, ignoreCase = true) }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSubSelected) Color(0xFF1877F2).copy(alpha = 0.22f) else Color(0xFF141414))
                                                .border(
                                                    1.dp,
                                                    if (isSubSelected) Color(0xFF1877F2) else Color(0xFF262626),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { activeRealitySubCategory = sub }
                                                .padding(horizontal = 9.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (catCount > 0) "$sub ($catCount)" else sub,
                                                fontSize = 11.sp,
                                                fontFamily = InterFontFamily,
                                                fontWeight = if (isSubSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSubSelected) Color(0xFF38BDF8) else Color(0xFF9CA3AF)
                                            )
                                        }
                                    }
                                }

                                val filteredRemoteBackgrounds = remember(remoteBackgrounds, activeRealitySubCategory) {
                                    if (activeRealitySubCategory == "All") remoteBackgrounds
                                    else remoteBackgrounds.filter { it.normalizedSubCategory().equals(activeRealitySubCategory, ignoreCase = true) }
                                }

                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isLoadingRemoteBgs && filteredRemoteBackgrounds.isEmpty()) {
                                        item(key = "story_reality_loading") {
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF1A1A1A)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color(0xFF1877F2)
                                                )
                                            }
                                        }
                                    } else if (filteredRemoteBackgrounds.isEmpty()) {
                                        item(key = "story_reality_retry") {
                                            Box(
                                                modifier = Modifier
                                                    .height(50.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF141414))
                                                    .border(1.dp, Color(0xFF2B2B2B), RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            BackgroundsApiService.fetchBackgrounds(context, forceRefresh = true)
                                                        }
                                                    }
                                                    .padding(horizontal = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (!remoteBgError.isNullOrBlank()) "Retry" else "Reload",
                                                        fontSize = 11.sp,
                                                        color = Color.White,
                                                        fontFamily = InterFontFamily,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        items(filteredRemoteBackgrounds, key = { "story_remote_${it.id}" }) { bg ->
                                            val isSelected = selectedRemoteBackground?.id == bg.id
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(
                                                        if (isSelected) 2.5.dp else 1.dp,
                                                        if (isSelected) Color.White else Color(0xFF2E2E2E),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        selectedTemplate = null
                                                        selectedRemoteBackground = bg
                                                        textTextColor = bg.resolvedDefaultTextColor()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Data-saver thumbnail only until selected
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
                                                            .size(9.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF38BDF8))
                                                            .border(1.dp, Color.White, CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                val selectedCat = AnimatedBgCategory.values().find { it.name == activeCategoryKey }
                                val templates = remember(activeCategoryKey) {
                                    if (selectedCat == null) emptyList()
                                    else ALL_TEXT_POST_BACKGROUNDS.filter {
                                        it.category == selectedCat && it.id != "none"
                                    }
                                }
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(templates, key = { it.id }) { bg ->
                                        val isSelected = selectedRemoteBackground == null && selectedTemplate?.id == bg.id
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Brush.linearGradient(bg.baseColors))
                                                .border(
                                                    if (isSelected) 2.5.dp else 1.dp,
                                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    selectedRemoteBackground = null
                                                    selectedTemplate = bg
                                                    textTextColor = bg.defaultTextColor
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (bg.iconEmoji.isNotBlank()) {
                                                Text(
                                                    text = bg.iconEmoji,
                                                    fontSize = 18.sp,
                                                    fontFamily = InterFontFamily
                                                )
                                            }
                                            if (bg.isAnimated) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(2.dp)
                                                        .size(9.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF38BDF8))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Mode Switcher & Action Icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mode Switcher: Text / Media
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF121212))
                                .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { mode = "text" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (mode == "text") Color(0xFF1877F2) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("story_mode_text_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    tint = if (mode == "text") Color.White else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Text",
                                    fontFamily = InterFontFamily,
                                    color = if (mode == "text") Color.White else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = { mode = "media" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (mode == "media") Color(0xFF1877F2) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("story_mode_media_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = if (mode == "media") Color.White else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Media",
                                    fontFamily = InterFontFamily,
                                    color = if (mode == "media") Color.White else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Right Action Icons: Aa Style (in Text mode), Camera, Media Plus, Music
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (mode == "text") {
                                IconButton(
                                    onClick = { showTextStyleControls = !showTextStyleControls },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (showTextStyleControls) Color(0xFF1877F2) else Color.White.copy(alpha = 0.12f)
                                        )
                                        .testTag("story_text_style_toggle")
                                ) {
                                    Text(
                                        text = "Aa",
                                        fontFamily = InterFontFamily,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { takePhotoLauncher.launch(null) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Take Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    pickMultipleMediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Media",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { showMusicPicker = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedMusic != null) Color(0xFF45BD62) else Color.White.copy(alpha = 0.12f)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Music",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Live Music Picker Modal
            if (showMusicPicker) {
                LiveMusicPickerModal(
                    onDismiss = { showMusicPicker = false },
                    onSelectSong = { song ->
                        selectedMusic = SelectedMusicInfo(
                            title = song.title,
                            artist = song.artist,
                            url = song.audioUrl,
                            coverUrl = song.cover
                        )
                        showMusicPicker = false
                    },
                    onSelectAudioFile = { uri, name ->
                        selectedMusic = SelectedMusicInfo(
                            title = name,
                            artist = "Local Upload",
                            url = uri.toString()
                        )
                        showMusicPicker = false
                    }
                )
            }
        }
    }
}

/**
 * Live Music Picker Modal matching web renderMusicPicker in Story.tsx:
 * - Upload Music button with system audio picker
 * - "F-MUSIC TRENDS" uppercase section
 * - Live fetch from /api/songs (no hardcoded fake tracks)
 * - Play/Pause audio preview
 * - Green "Add" button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMusicPickerModal(
    onDismiss: () -> Unit,
    onSelectSong: (Song) -> Unit,
    onSelectAudioFile: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    var liveSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var previewSongId by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Audio file picker launcher
    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = try {
                var name = "Uploaded Audio"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx)
                    }
                }
                name
            } catch (_: Exception) { "Uploaded Audio" }
            onSelectAudioFile(uri, fileName.substringBeforeLast("."))
        }
    }

    // Fetch live songs from /api/songs
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val songs = StoriesApiService.fetchSongs()
            liveSongs = songs
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.bgScreen
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.headerBg)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Close",
                            tint = AppColors.headerIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Close",
                            fontFamily = InterFontFamily,
                            color = AppColors.headerText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Add Music",
                        fontFamily = InterFontFamily,
                        color = AppColors.headerText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.width(48.dp))
                }

                HorizontalDivider(color = AppColors.headerDivider, thickness = 1.dp)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Upload Music button (uses native picker)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, AppColors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pickAudioLauncher.launch("audio/*") }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.primaryBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload Music",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Upload Music",
                                        fontFamily = InterFontFamily,
                                        color = AppColors.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Choose a file from your device",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // F-Music Trends Header
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "F-MUSIC TRENDS",
                            fontFamily = InterFontFamily,
                            color = AppColors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = AppColors.primaryBlue,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Loading music...",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else if (liveSongs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No tracks currently listed. Upload your own music above!",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.textSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(liveSongs, key = { it.id }) { song ->
                            val isThisPlaying = previewSongId == song.id
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AppColors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = song.cover.ifBlank { "https://media.featheredsocial.site/task_01kftb3024ed7bm84gy6j485fh_1769336848_img_0.webp" },
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            fontFamily = InterFontFamily,
                                            color = AppColors.textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            color = AppColors.textSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Play / Pause Preview Button
                                        IconButton(
                                            onClick = {
                                                if (isThisPlaying) {
                                                    exoPlayer.pause()
                                                    previewSongId = null
                                                } else {
                                                    exoPlayer.stop()
                                                    exoPlayer.clearMediaItems()
                                                    if (song.audioUrl.isNotBlank()) {
                                                        exoPlayer.setMediaItem(MediaItem.fromUri(song.audioUrl))
                                                        exoPlayer.prepare()
                                                        exoPlayer.play()
                                                        previewSongId = song.id
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1877F2))
                                        ) {
                                            Icon(
                                                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isThisPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Green "Add" Button
                                        Button(
                                            onClick = {
                                                exoPlayer.stop()
                                                previewSongId = null
                                                onSelectSong(song)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF45BD62)),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(
                                                text = "Add",
                                                fontFamily = InterFontFamily,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible overload for CreateStoryDialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onCreateStory: (mediaUrl: String, type: String, textContent: String, backgroundStyle: String, musicTitle: String?) -> Unit
) {
    CreateStoryDialog(
        currentUser = currentUser,
        onDismiss = onDismiss,
        onSubmitStory = { uri, isVideo, type, text, bg, music, _ ->
            onCreateStory(uri?.toString() ?: "", type, text, bg, music)
        }
    )
}
