package com.unera.social.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import com.unera.social.data.BackgroundsApiService
import com.unera.social.data.MarketplaceApiService
import com.unera.social.data.MockData
import com.unera.social.data.RemotePostBackground
import com.unera.social.ui.components.ALL_TEXT_POST_BACKGROUNDS
import com.unera.social.ui.components.AnimatedBgCategory
import com.unera.social.ui.components.AnimatedPostBackground
import com.unera.social.ui.components.CommentActionModal
import com.unera.social.ui.components.RemoteImageBackgroundLayer
import com.unera.social.ui.components.TextPostBackgroundTemplate
import com.unera.social.ui.components.VerifiedBadge
import com.unera.social.ui.navigation.CoordinatedDestinationReveal
import com.unera.social.ui.navigation.DestinationType
import com.unera.social.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

enum class CreatePostView {
    MAIN, TAG, FEELING, LOCATION
}

data class BackgroundColorOption(
    val id: String,
    val label: String,
    val colors: List<Color>,
    val styleString: String
)

val CREATE_POST_BACKGROUNDS = listOf(
    BackgroundColorOption("none", "None", emptyList(), ""),
    BackgroundColorOption("red", "Red", listOf(Color(0xFFFF0057), Color(0xFFE64C4C)), "linear-gradient(45deg, #FF0057, #E64C4C)"),
    BackgroundColorOption("blue", "Blue", listOf(Color(0xFF00C6FF), Color(0xFF0072FF)), "linear-gradient(45deg, #00C6FF, #0072FF)"),
    BackgroundColorOption("green", "Green", listOf(Color(0xFFA8FF78), Color(0xFF78FFD6)), "linear-gradient(45deg, #a8ff78, #78ffd6)"),
    BackgroundColorOption("purple", "Purple", listOf(Color(0xFFE65C00), Color(0xFFF9D423)), "linear-gradient(45deg, #e65c00, #F9D423)"),
    BackgroundColorOption("dark", "Dark", listOf(Color(0xFF434343), Color(0xFF000000)), "linear-gradient(to right, #434343 0%, black 100%)"),
    BackgroundColorOption("fire", "Fire", listOf(Color(0xFFF6D365), Color(0xFFFDA085)), "linear-gradient(120deg, #f6d365 0%, #fda085 100%)")
)

val POST_FEELINGS = listOf(
    "Happy 😊", "Blessed ✨", "Loved ❤️", "Sad 😢", "Excited 🚀",
    "Thankful 🙏", "Crazy 😜", "Tired 😴", "Cool 😎", "Relaxed 😌"
)

val NEARBY_LOCATIONS = listOf(
    "Dar es Salaam, Tanzania", "Zanzibar, Tanzania", "Arusha, Tanzania",
    "Nairobi, Kenya", "Mwanza, Tanzania", "Dodoma, Tanzania",
    "Kigali, Rwanda", "Kampala, Uganda", "Moshi, Tanzania"
)

@Composable
fun CreatePostModal(
    currentUser: User,
    allUsers: List<User> = emptyList(),
    initialMediaUrl: String? = null,
    initialOpenPicker: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (content: String, mediaUrl: String?, mediaUrls: List<String>, feeling: String?, location: String?, backgroundStyle: String?, taggedUsers: List<Long>, mediaType: String?) -> Unit,
    onCreateEventClick: (() -> Unit)? = null,
    onPhotoClick: (() -> Unit)? = null,
    onVideoClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val remoteBackgrounds by BackgroundsApiService.backgrounds.collectAsState()
    val isLoadingRemoteBgs by BackgroundsApiService.isLoading.collectAsState()
    val remoteBgError by BackgroundsApiService.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        BackgroundsApiService.fetchBackgrounds(context, forceRefresh = true)
    }

    var view by remember { mutableStateOf(CreatePostView.MAIN) }
    var content by remember { mutableStateOf("") }
    var mediaUrls by remember { mutableStateOf(if (!initialMediaUrl.isNullOrBlank()) listOf(initialMediaUrl) else emptyList<String>()) }
    var isVideoMedia by remember { mutableStateOf(initialOpenPicker == "video") }
    var activeBackground by remember { mutableStateOf<TextPostBackgroundTemplate?>(null) }
    var activeRemoteBackground by remember { mutableStateOf<RemotePostBackground?>(null) }
    var activeCategory by remember { mutableStateOf(AnimatedBgCategory.REALISTIC) }
    var activeRealitySubCategory by remember { mutableStateOf("All") }
    var textAlignment by remember { mutableStateOf(TextAlign.Center) }
    var textPosition by remember { mutableStateOf("center") }
    var textTextColor by remember { mutableStateOf(Color.White) }
    var textFontSizeSp by remember { mutableStateOf(23) }
    var textFontStyle by remember { mutableStateOf("sans") }
    var textIsBold by remember { mutableStateOf(true) }
    var selectedFeeling by remember { mutableStateOf<String?>(null) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var locationQuery by remember { mutableStateOf("") }
    var taggedUsers by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var tagSearchQuery by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isVideoMedia = false
            mediaUrls = mediaUrls + uris.map { it.toString() }
            activeBackground = null
            activeRemoteBackground = null
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isVideoMedia = true
            mediaUrls = mediaUrls + uri.toString()
            activeBackground = null
            activeRemoteBackground = null
        }
    }

    var hasLaunchedInitialPicker by remember { mutableStateOf(false) }
    LaunchedEffect(initialOpenPicker) {
        if (!hasLaunchedInitialPicker && initialOpenPicker != null) {
            hasLaunchedInitialPicker = true
            if (initialOpenPicker == "video") {
                videoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            } else {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }

    val hasActiveBackground = (activeRemoteBackground != null) || (activeBackground != null && activeBackground?.id != "none")
    val canPost = content.isNotBlank() || mediaUrls.isNotEmpty() || hasActiveBackground

    // Link detection for link preview
    val detectedUrl = remember(content) {
        val regex = Regex("""https?://[^\s]+""")
        regex.find(content)?.value
    }

    val buildFinalBackgroundStyle: () -> String? = {
        // If there is media (images or videos), NEVER apply any background style or colors!
        if (mediaUrls.isNotEmpty()) {
            null
        } else {
            val alignStr = when (textAlignment) {
                TextAlign.Start -> "left"
                TextAlign.End -> "right"
                else -> "center"
            }
            val colorHex = String.format("#%06X", (0xFFFFFF and textTextColor.toArgb()))
            val weightStr = if (textIsBold) "bold" else "normal"
            val remote = activeRemoteBackground
            val bg = activeBackground

            if (remote != null) {
                "reality:${remote.id}|bgtype:${remote.backgroundTypeValue}|align:$alignStr|pos:$textPosition|color:$colorHex|size:$textFontSizeSp|font:$textFontStyle|weight:$weightStr"
            } else if (bg != null && bg.id != "none" && bg.styleString.isNotBlank()) {
                "${bg.styleString}|align:$alignStr|pos:$textPosition|color:$colorHex|size:$textFontSizeSp|font:$textFontStyle|weight:$weightStr"
            } else {
                null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.bgScreen)
                .testTag("create_post_modal")
        ) {
            when (view) {
                CreatePostView.TAG -> {
                    // Tag People Sub-view
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(AppColors.headerBg)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { view = CreatePostView.MAIN }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppColors.headerIconTint
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Post with",
                                fontSize = 20.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.headerText,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { view = CreatePostView.MAIN }) {
                                Text(
                                    text = "Done",
                                    color = if (AppColors.isLight) Color.White else AppColors.primaryBlue,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = tagSearchQuery,
                            onValueChange = { tagSearchQuery = it },
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal
                            ),
                            placeholder = { Text("Search friends…", color = AppColors.textSecondary, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppColors.textSecondary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppColors.bgInput,
                                unfocusedContainerColor = AppColors.bgInput,
                                focusedTextColor = AppColors.textPrimary,
                                unfocusedTextColor = AppColors.textPrimary,
                                focusedBorderColor = AppColors.primaryBlue,
                                unfocusedBorderColor = AppColors.border
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        val filteredFriends = allUsers.filter { it.id != currentUser.id && (tagSearchQuery.isBlank() || it.name.contains(tagSearchQuery, ignoreCase = true) || it.username.contains(tagSearchQuery, ignoreCase = true)) }

                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            items(filteredFriends, key = { it.id }) { friend ->
                                val isTagged = friend.id in taggedUsers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            taggedUsers = if (isTagged) taggedUsers - friend.id else taggedUsers + friend.id
                                        }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = friend.profileImageUrl,
                                        contentDescription = friend.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, AppColors.border, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(friend.name, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppColors.textPrimary)
                                        Text(friend.username, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppColors.textSecondary)
                                    }
                                    Checkbox(
                                        checked = isTagged,
                                        onCheckedChange = {
                                            taggedUsers = if (isTagged) taggedUsers - friend.id else taggedUsers + friend.id
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = AppColors.primaryBlue,
                                            uncheckedColor = AppColors.textMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                CreatePostView.FEELING -> {
                    // Feeling Sub-view
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(AppColors.headerBg)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { view = CreatePostView.MAIN }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppColors.headerIconTint
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How are you feeling?",
                                fontSize = 20.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.headerText
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(POST_FEELINGS) { feel ->
                                val isSel = selectedFeeling == feel
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) AppColors.primaryBlue else AppColors.bgCard
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) AppColors.primaryBlue else AppColors.border),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFeeling = if (isSel) null else feel
                                            view = CreatePostView.MAIN
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = feel,
                                            fontSize = 17.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSel) Color.White else AppColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CreatePostView.LOCATION -> {
                    // Location Sub-view
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(AppColors.headerBg)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { view = CreatePostView.MAIN }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppColors.headerIconTint
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Search Location",
                                fontSize = 20.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.headerText
                            )
                        }

                        OutlinedTextField(
                            value = locationQuery,
                            onValueChange = { locationQuery = it },
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal
                            ),
                            placeholder = { Text("Where are you?", color = AppColors.textSecondary, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFF02849)) },
                            trailingIcon = {
                                if (locationQuery.isNotBlank()) {
                                    IconButton(onClick = { locationQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = AppColors.textSecondary)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppColors.bgInput,
                                unfocusedContainerColor = AppColors.bgInput,
                                focusedTextColor = AppColors.textPrimary,
                                unfocusedTextColor = AppColors.textPrimary,
                                focusedBorderColor = AppColors.primaryBlue,
                                unfocusedBorderColor = AppColors.border
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        val matchedLocations = remember(locationQuery) {
                            if (locationQuery.isBlank()) NEARBY_LOCATIONS
                            else (NEARBY_LOCATIONS.filter { it.contains(locationQuery, ignoreCase = true) } + listOf(locationQuery)).distinct()
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            item {
                                Text(
                                    text = "NEARBY SUGGESTIONS",
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.textSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(matchedLocations) { loc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedLocation = loc
                                            view = CreatePostView.MAIN
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFF02849), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = loc,
                                        fontSize = 15.sp,
                                        fontFamily = InterFontFamily,
                                        color = AppColors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)
                            }
                        }
                    }
                }

                CreatePostView.MAIN -> {
                    // Main Create Post Screen
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(AppColors.headerBg)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = AppColors.headerIconTint
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create Post",
                                    fontSize = 20.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.headerText
                                )
                            }

                            Button(
                                onClick = {
                                    if (canPost) {
                                        val backgroundStyleValue = if (mediaUrls.isNotEmpty()) null else buildFinalBackgroundStyle()
                                        val resolvedType = if (mediaUrls.isNotEmpty()) (if (isVideoMedia) "video" else "image") else null
                                        onSubmit(
                                            content,
                                            mediaUrls.firstOrNull(),
                                            mediaUrls,
                                            selectedFeeling,
                                            selectedLocation,
                                            backgroundStyleValue,
                                            taggedUsers.toList(),
                                            resolvedType
                                        )
                                        onDismiss()
                                    }
                                },
                                enabled = canPost,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (AppColors.isLight) Color.White else AppColors.primaryBlue,
                                    disabledContainerColor = if (AppColors.isLight) Color.White.copy(alpha = 0.35f) else AppColors.bgChip
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "POST",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (canPost) {
                                        if (AppColors.isLight) AppColors.primaryBlue else Color.White
                                    } else {
                                        if (AppColors.isLight) Color.White.copy(alpha = 0.75f) else AppColors.textSecondary
                                    }
                                )
                            }
                        }

                        // Scrollable content area
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            // User Info Row
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = currentUser.profileImageUrl,
                                        contentDescription = currentUser.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, AppColors.border, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = currentUser.name,
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 19.sp,
                                                color = AppColors.textPrimary
                                            )
                                            if (selectedFeeling != null) {
                                                Text(
                                                    text = " is feeling $selectedFeeling",
                                                    fontSize = 14.sp,
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    color = AppColors.textBody
                                                )
                                            }
                                        }

                                        if (selectedLocation != null || taggedUsers.isNotEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (selectedLocation != null) {
                                                    Text(
                                                        text = "in ${selectedLocation?.substringBefore(",")}",
                                                        fontSize = 12.sp,
                                                        fontFamily = InterFontFamily,
                                                        fontWeight = FontWeight.Normal,
                                                        color = AppColors.textSecondary
                                                    )
                                                }
                                                if (taggedUsers.isNotEmpty()) {
                                                    if (selectedLocation != null) Text(" • ", color = AppColors.textSecondary, fontSize = 12.sp, fontFamily = InterFontFamily)
                                                    Text(
                                                        text = "with ${taggedUsers.size} other${if (taggedUsers.size > 1) "s" else ""}",
                                                        fontSize = 12.sp,
                                                        fontFamily = InterFontFamily,
                                                        color = AppColors.primaryBlue,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AppColors.bgChip)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Public, contentDescription = null, tint = AppColors.textSecondary, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Public", fontSize = 11.sp, color = AppColors.textPrimary, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }

                            // Colored / Animated / Reality Image Canvas or Normal Text Input
                            item {
                                if (hasActiveBackground) {
                                    val currentBg = activeBackground
                                    val currentRemote = activeRemoteBackground
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

                                    // Full bleed animated, Reality image, or colored canvas
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 290.dp)
                                            .padding(horizontal = 0.dp, vertical = 6.dp)
                                            .clip(RoundedCornerShape(0.dp))
                                            .then(
                                                if (currentRemote != null || (currentBg != null && currentBg.isAnimated)) Modifier
                                                else if (currentBg != null) Modifier.background(Brush.linearGradient(currentBg.baseColors))
                                                else Modifier
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
                                        } else if (currentBg != null && currentBg.isAnimated) {
                                            AnimatedPostBackground(
                                                templateId = currentBg.id,
                                                modifier = Modifier.matchParentSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(Color.Black.copy(alpha = 0.22f))
                                            )
                                        }

                                        // Top badge: background label
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                val badgeLabel = when {
                                                    currentRemote != null -> "${if (currentRemote.isAnimated) "✨" else "🖼️"} ${currentRemote.name}"
                                                    currentBg != null -> "${currentBg.iconEmoji} ${currentBg.label}"
                                                    else -> ""
                                                }
                                                Text(
                                                    text = badgeLabel,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        // Clear background button
                                        IconButton(
                                            onClick = {
                                                activeBackground = null
                                                activeRemoteBackground = null
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .testTag("clear_post_background_btn")
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear background", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }

                                        OutlinedTextField(
                                            value = content,
                                            onValueChange = { content = it },
                                            placeholder = {
                                                Text(
                                                    "What's on your mind?",
                                                    color = textTextColor.copy(alpha = 0.72f),
                                                    fontSize = textFontSizeSp.sp,
                                                    fontFamily = editorFontFamily,
                                                    fontWeight = editorFontWeight,
                                                    fontStyle = editorFontStyle,
                                                    textAlign = textAlignment,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedTextColor = textTextColor,
                                                unfocusedTextColor = textTextColor
                                            ),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = textFontSizeSp.sp,
                                                lineHeight = (textFontSizeSp * 1.4f).sp,
                                                fontWeight = editorFontWeight,
                                                fontFamily = editorFontFamily,
                                                fontStyle = editorFontStyle,
                                                textAlign = textAlignment,
                                                color = textTextColor,
                                                shadow = editorTextShadow
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 24.dp,
                                                    end = 24.dp,
                                                    top = if (textPosition == "top") 42.dp else 28.dp,
                                                    bottom = 24.dp
                                                )
                                                .testTag("background_post_text_input")
                                        )
                                    }
                                } else {
                                    // Normal text input
                                    OutlinedTextField(
                                        value = content,
                                        onValueChange = { content = it },
                                        placeholder = { Text("What's on your mind?", color = AppColors.textMuted, fontSize = 20.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp)
                                            .heightIn(min = 160.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = AppColors.textPrimary,
                                            unfocusedTextColor = AppColors.textPrimary
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 20.sp,
                                            lineHeight = 28.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            color = AppColors.textPrimary
                                        )
                                    )
                                }
                            }

                            // Attached Media Previews
                            if (mediaUrls.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        mediaUrls.forEachIndexed { index, url ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(AppColors.bgCard)
                                                    .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                                            ) {
                                                AsyncImage(
                                                    model = url,
                                                    contentDescription = "Attachment ${index + 1}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Video Play Indicator Overlay
                                                val isVideoItem = url.contains("video", ignoreCase = true) ||
                                                        url.endsWith(".mp4", ignoreCase = true) ||
                                                        url.endsWith(".mov", ignoreCase = true)
                                                if (isVideoItem) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(52.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.Black.copy(alpha = 0.65f))
                                                            .align(Alignment.Center),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Video",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(34.dp)
                                                        )
                                                    }
                                                }

                                                // Number tag (1, 2, 3...)
                                                Box(
                                                    modifier = Modifier
                                                        .padding(8.dp)
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1877F2))
                                                        .align(Alignment.TopStart),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                                                }

                                                // Remove button
                                                IconButton(
                                                    onClick = {
                                                        mediaUrls = mediaUrls.filterIndexed { i, _ -> i != index }
                                                    },
                                                    modifier = Modifier
                                                        .padding(8.dp)
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.6f))
                                                        .align(Alignment.TopEnd)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        // Quick buttons to add more media directly from Gallery
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                                border = BorderStroke(1.dp, AppColors.border),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Photos", fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    videoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                    )
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF43F5E)),
                                                border = BorderStroke(1.dp, AppColors.border),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Video", fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }

                            // Link Preview Card
                            if (detectedUrl != null && mediaUrls.isEmpty()) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                                        border = BorderStroke(1.dp, AppColors.border),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(AppColors.primaryBlue.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Link, contentDescription = null, tint = AppColors.primaryBlue)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Link Preview", fontSize = 13.sp, color = AppColors.textSecondary, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                                                Text(detectedUrl, fontSize = 14.sp, color = AppColors.primaryBlue, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }

                            // Background Swatches & Animated Themes Selector (if no media)
                            if (mediaUrls.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp)
                                    ) {
                                        // 1. Text styling controls (visible when a background is active)
                                        if (hasActiveBackground) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // Top control row: Alignment, Vertical Position, Bold toggle, Font family
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Horizontal Alignment (Left, Center, Right)
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(AppColors.bgChip)
                                                            .padding(2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = { textAlignment = TextAlign.Start },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(if (textAlignment == TextAlign.Start) AppColors.primaryBlue else Color.Transparent, RoundedCornerShape(6.dp))
                                                                .testTag("bg_align_left")
                                                        ) {
                                                            Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Left", tint = if (textAlignment == TextAlign.Start) Color.White else AppColors.textPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                        IconButton(
                                                            onClick = { textAlignment = TextAlign.Center },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(if (textAlignment == TextAlign.Center) AppColors.primaryBlue else Color.Transparent, RoundedCornerShape(6.dp))
                                                                .testTag("bg_align_center")
                                                        ) {
                                                            Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center", tint = if (textAlignment == TextAlign.Center) Color.White else AppColors.textPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                        IconButton(
                                                            onClick = { textAlignment = TextAlign.End },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(if (textAlignment == TextAlign.End) AppColors.primaryBlue else Color.Transparent, RoundedCornerShape(6.dp))
                                                                .testTag("bg_align_right")
                                                        ) {
                                                            Icon(Icons.Default.FormatAlignRight, contentDescription = "Align Right", tint = if (textAlignment == TextAlign.End) Color.White else AppColors.textPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                    }

                                                    // Vertical Text Positioning (Top, Center, Bottom)
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(AppColors.bgChip)
                                                            .padding(2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = { textPosition = "top" },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(if (textPosition == "top") AppColors.primaryBlue else Color.Transparent, RoundedCornerShape(6.dp))
                                                                .testTag("bg_pos_top")
                                                        ) {
                                                            Icon(Icons.Default.VerticalAlignTop, contentDescription = "Position Top", tint = if (textPosition == "top") Color.White else AppColors.textPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                        IconButton(
                                                            onClick = { textPosition = "center" },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(if (textPosition == "center") AppColors.primaryBlue else Color.Transparent, RoundedCornerShape(6.dp))
                                                                .testTag("bg_pos_center")
                                                        ) {
                                                            Icon(Icons.Default.VerticalAlignCenter, contentDescription = "Position Center", tint = if (textPosition == "center") Color.White else AppColors.textPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                        IconButton(
                                                            onClick = { textPosition = "bottom" },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(if (textPosition == "bottom") AppColors.primaryBlue else Color.Transparent, RoundedCornerShape(6.dp))
                                                                .testTag("bg_pos_bottom")
                                                        ) {
                                                            Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Position Bottom", tint = if (textPosition == "bottom") Color.White else AppColors.textPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                    }

                                                    // Bold toggle + Font Style cycle
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(30.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (textIsBold) AppColors.primaryBlue else AppColors.bgChip)
                                                                .clickable { textIsBold = !textIsBold }
                                                                .testTag("bg_bold_toggle"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "B",
                                                                fontSize = 14.sp,
                                                                fontFamily = InterFontFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (textIsBold) Color.White else AppColors.textPrimary
                                                            )
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(AppColors.bgChip)
                                                                .clickable {
                                                                    textFontStyle = when (textFontStyle) {
                                                                        "sans" -> "serif"
                                                                        "serif" -> "display"
                                                                        "display" -> "italic"
                                                                        "italic" -> "cursive"
                                                                        else -> "sans"
                                                                    }
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                                .testTag("bg_font_style")
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
                                                                color = if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF38BDF8)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Second control row: Text color swatches + Auto contrast + Font size stepper
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Auto Contrast Button
                                                        Box(
                                                            modifier = Modifier
                                                                .height(24.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(AppColors.bgCard)
                                                                .border(1.dp, if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                                                                .clickable {
                                                                    textTextColor = activeRemoteBackground?.resolvedDefaultTextColor()
                                                                        ?: activeBackground?.defaultTextColor
                                                                        ?: Color.White
                                                                }
                                                                .padding(horizontal = 6.dp)
                                                                .testTag("bg_color_auto"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "Auto",
                                                                fontSize = 10.sp,
                                                                fontFamily = InterFontFamily,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF38BDF8)
                                                            )
                                                        }

                                                        val colors = listOf(
                                                            Color.White,
                                                            Color(0xFFFEF08A),
                                                            Color(0xFFA5F3FC),
                                                            Color(0xFFFECDD3),
                                                            Color(0xFFBBF7D0),
                                                            Color(0xFF0F172A)
                                                        )
                                                        for ((cIdx, c) in colors.withIndex()) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(22.dp)
                                                                    .clip(CircleShape)
                                                                    .background(c)
                                                                    .border(if (textTextColor == c) 2.dp else 1.dp, if (textTextColor == c) AppColors.primaryBlue else AppColors.borderStrong, CircleShape)
                                                                    .clickable { textTextColor = c }
                                                                    .testTag("bg_color_swatch_$cIdx")
                                                            )
                                                        }
                                                    }

                                                    // Font size stepper (16..32sp)
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(AppColors.bgChip)
                                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .clickable { if (textFontSizeSp > 16) textFontSizeSp -= 2 }
                                                                .testTag("bg_font_size_dec"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("A-", fontSize = 11.sp, color = AppColors.textPrimary, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                                                        }
                                                        Text(
                                                            text = "${textFontSizeSp}sp",
                                                            fontSize = 11.sp,
                                                            color = AppColors.textSecondary,
                                                            fontFamily = InterFontFamily,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .clickable { if (textFontSizeSp < 32) textFontSizeSp += 2 }
                                                                .testTag("bg_font_size_inc"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("A+", fontSize = 12.sp, color = AppColors.textPrimary, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 2. Category selection chips
                                        LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            items(AnimatedBgCategory.values()) { cat ->
                                                val isCatSelected = activeCategory == cat
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(if (isCatSelected) AppColors.primaryBlue else AppColors.bgChip)
                                                        .clickable {
                                                            activeCategory = cat
                                                            if (cat == AnimatedBgCategory.REALITY && !isLoadingRemoteBgs) {
                                                                coroutineScope.launch {
                                                                    BackgroundsApiService.fetchBackgrounds(context, forceRefresh = true)
                                                                }
                                                            }
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                        .testTag("bg_category_${cat.name.lowercase()}")
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(cat.emoji, fontSize = 12.sp, fontFamily = InterFontFamily)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = cat.label,
                                                            fontSize = 12.sp,
                                                            fontFamily = InterFontFamily,
                                                            fontWeight = if (isCatSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                            color = if (isCatSelected) Color.White else AppColors.textPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // 3. Thumbnails for the active category
                                        if (activeCategory == AnimatedBgCategory.REALITY) {
                                            val realitySubCats = remember(remoteBackgrounds) {
                                                val cats = remoteBackgrounds.map { it.normalizedSubCategory() }.distinct().filter { it != "All" }
                                                if (cats.isNotEmpty()) listOf("All") + cats else listOf("All")
                                            }

                                            LazyRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 2.dp)
                                                    .testTag("reality_categories_row"),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                item(key = "reality_refresh_chip") {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(AppColors.bgCard)
                                                            .border(1.dp, AppColors.borderStrong, RoundedCornerShape(12.dp))
                                                            .clickable(enabled = !isLoadingRemoteBgs) {
                                                                coroutineScope.launch {
                                                                    BackgroundsApiService.fetchBackgrounds(context, forceRefresh = true)
                                                                }
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                                            .testTag("reality_refresh_chip")
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            if (isLoadingRemoteBgs) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(12.dp),
                                                                    strokeWidth = 1.5.dp,
                                                                    color = AppColors.primaryBlue
                                                                )
                                                            } else {
                                                                Icon(
                                                                    imageVector = Icons.Default.Refresh,
                                                                    contentDescription = "Refresh Reality backgrounds",
                                                                    tint = AppColors.primaryBlue,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                            }
                                                            Text(
                                                                text = "Sync",
                                                                fontSize = 11.sp,
                                                                fontFamily = InterFontFamily,
                                                                fontWeight = FontWeight.Medium,
                                                                color = AppColors.primaryBlue
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
                                                            .background(if (isSubSelected) AppColors.bgBlueTint else AppColors.bgCard)
                                                            .border(
                                                                1.dp,
                                                                if (isSubSelected) AppColors.primaryBlue else AppColors.border,
                                                                RoundedCornerShape(12.dp)
                                                            )
                                                            .clickable { activeRealitySubCategory = sub }
                                                            .padding(horizontal = 9.dp, vertical = 5.dp)
                                                            .testTag("reality_subcat_${sub.lowercase()}")
                                                    ) {
                                                        Text(
                                                            text = if (catCount > 0) "$sub ($catCount)" else sub,
                                                            fontSize = 11.sp,
                                                            fontFamily = InterFontFamily,
                                                            fontWeight = if (isSubSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                            color = if (isSubSelected) (if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF38BDF8)) else AppColors.textSecondary
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
                                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                                    .testTag("reality_backgrounds_gallery"),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                item(key = "reality_none") {
                                                    val isNoneSelected = !hasActiveBackground
                                                    Box(
                                                        modifier = Modifier
                                                            .size(54.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(AppColors.bgCard)
                                                            .border(if (isNoneSelected) 2.dp else 1.dp, if (isNoneSelected) AppColors.primaryBlue else AppColors.borderStrong, RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                activeBackground = null
                                                                activeRemoteBackground = null
                                                            }
                                                            .testTag("reality_bg_none"),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.FormatColorReset, contentDescription = "None", tint = AppColors.textPrimary, modifier = Modifier.size(18.dp))
                                                            Text("None", fontSize = 9.sp, color = AppColors.textSecondary, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                                                        }
                                                    }
                                                }

                                                if (isLoadingRemoteBgs && filteredRemoteBackgrounds.isEmpty()) {
                                                    item(key = "reality_loading") {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(54.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(AppColors.bgChip),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(20.dp),
                                                                strokeWidth = 2.dp,
                                                                color = AppColors.primaryBlue
                                                            )
                                                        }
                                                    }
                                                } else if (filteredRemoteBackgrounds.isEmpty()) {
                                                    item(key = "reality_retry") {
                                                        Box(
                                                            modifier = Modifier
                                                                .height(54.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(AppColors.bgCard)
                                                                .border(1.dp, AppColors.borderStrong, RoundedCornerShape(10.dp))
                                                                .clickable {
                                                                    coroutineScope.launch {
                                                                        BackgroundsApiService.fetchBackgrounds(context, forceRefresh = true)
                                                                    }
                                                                }
                                                                .padding(horizontal = 12.dp)
                                                                .testTag("reality_retry_btn"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = AppColors.primaryBlue, modifier = Modifier.size(16.dp))
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = if (!remoteBgError.isNullOrBlank()) "Retry" else "Reload",
                                                                    fontSize = 11.sp,
                                                                    color = AppColors.textPrimary,
                                                                    fontFamily = InterFontFamily,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    items(filteredRemoteBackgrounds, key = { "remote_${it.id}" }) { bg ->
                                                        val isSelected = activeRemoteBackground?.id == bg.id
                                                        Box(
                                                            modifier = Modifier
                                                                .size(54.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .border(
                                                                    if (isSelected) 2.5.dp else 1.dp,
                                                                    if (isSelected) (if (AppColors.isLight) AppColors.primaryBlue else Color.White) else AppColors.borderStrong,
                                                                    RoundedCornerShape(10.dp)
                                                                )
                                                                .clickable {
                                                                    activeBackground = null
                                                                    activeRemoteBackground = bg
                                                                    textTextColor = bg.resolvedDefaultTextColor()
                                                                }
                                                                .testTag("reality_bg_item_${bg.id}"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            // Data saver: render strictly thumbnail_url (0 KB full feed image downloaded until clicked)
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
                                                                        .size(10.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color(0xFF38BDF8))
                                                                        .border(1.dp, Color.White, CircleShape)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            val currentTemplates = remember(activeCategory) {
                                                ALL_TEXT_POST_BACKGROUNDS.filter { it.category == activeCategory }
                                            }

                                            LazyRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                items(currentTemplates) { bg ->
                                                    val isSelected = activeRemoteBackground == null &&
                                                        (activeBackground?.id == bg.id || (activeBackground == null && bg.id == "none"))

                                                    if (bg.id == "none") {
                                                        // "None" Reset button
                                                        Box(
                                                            modifier = Modifier
                                                                .size(46.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(AppColors.bgCard)
                                                                .border(if (isSelected) 2.dp else 1.dp, if (isSelected) AppColors.primaryBlue else AppColors.borderStrong, RoundedCornerShape(10.dp))
                                                                .clickable {
                                                                    activeBackground = null
                                                                    activeRemoteBackground = null
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Icon(Icons.Default.FormatColorReset, contentDescription = "None", tint = AppColors.textPrimary, modifier = Modifier.size(18.dp))
                                                                Text("None", fontSize = 9.sp, color = AppColors.textSecondary, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                                                            }
                                                        }
                                                    } else {
                                                        // Rich thumbnail with gradient base + emoji icon badge + selection ring
                                                        Box(
                                                            modifier = Modifier
                                                                .size(46.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(Brush.linearGradient(bg.baseColors))
                                                                .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) (if (AppColors.isLight) AppColors.primaryBlue else Color.White) else AppColors.borderStrong, RoundedCornerShape(10.dp))
                                                                .clickable {
                                                                    activeRemoteBackground = null
                                                                    activeBackground = bg
                                                                    textTextColor = bg.defaultTextColor
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = bg.iconEmoji,
                                                                fontSize = 18.sp,
                                                                fontFamily = InterFontFamily
                                                            )
                                                            if (bg.isAnimated) {
                                                                // Small animated sparkle indicator badge
                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.BottomEnd)
                                                                        .padding(2.dp)
                                                                        .size(10.dp)
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
                                }
                            }

                            // Options Items List
                            item {
                                HorizontalDivider(color = AppColors.border, thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // 1. Photo
                                    CreatePostOptionRow(
                                        icon = Icons.Default.Image,
                                        iconTint = Color(0xFF45BD62),
                                        label = "Photo",
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    )

                                    // 2. Video
                                    CreatePostOptionRow(
                                        icon = Icons.Default.CameraAlt,
                                        iconTint = Color(0xFFF3425F),
                                        label = "Video",
                                        onClick = {
                                            videoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        }
                                    )

                                    // 3. Post with
                                    CreatePostOptionRow(
                                        icon = Icons.Default.PersonAdd,
                                        iconTint = Color(0xFF1877F2),
                                        label = "Post with",
                                        onClick = { view = CreatePostView.TAG }
                                    )

                                    // 4. Feeling/activity
                                    CreatePostOptionRow(
                                        icon = Icons.Default.Mood,
                                        iconTint = Color(0xFFF7B928),
                                        label = "Feeling/activity",
                                        onClick = { view = CreatePostView.FEELING }
                                    )

                                    // 5. Check in
                                    CreatePostOptionRow(
                                        icon = Icons.Default.LocationOn,
                                        iconTint = Color(0xFFF02849),
                                        label = "Check in",
                                        onClick = { view = CreatePostView.LOCATION }
                                    )

                                    // 6. Create Event
                                    CreatePostOptionRow(
                                        icon = Icons.Default.Event,
                                        iconTint = Color(0xFFF59E0B),
                                        label = "Create Event",
                                        onClick = {
                                            onDismiss()
                                            onCreateEventClick?.invoke()
                                        }
                                    )
                                }
                            }
                        }

                        // Bottom Action Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.bgSurface)
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (canPost) {
                                        val resolvedType = if (mediaUrls.isNotEmpty()) (if (isVideoMedia) "video" else "image") else null
                                        val backgroundStyleValue = if (mediaUrls.isNotEmpty()) null else buildFinalBackgroundStyle()
                                        onSubmit(
                                            content,
                                            mediaUrls.firstOrNull(),
                                            mediaUrls,
                                            selectedFeeling,
                                            selectedLocation,
                                            backgroundStyleValue,
                                            taggedUsers.toList(),
                                            resolvedType
                                        )
                                        onDismiss()
                                    }
                                },
                                enabled = canPost,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_post_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.primaryBlue,
                                    disabledContainerColor = AppColors.bgChip
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "POST",
                                    fontSize = 16.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = if (canPost) Color.White else AppColors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePostOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            color = AppColors.textPrimary
        )
    }
}

/**
 * Backward-compatible overload for CreatePostDialog
 */
@Composable
fun CreatePostDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmit: (content: String, mediaUrl: String?, feeling: String?) -> Unit
) {
    CreatePostModal(
        currentUser = currentUser,
        onDismiss = onDismiss,
        onSubmit = { content, mediaUrl, _, feeling, _, _, _, _ ->
            onSubmit(content, mediaUrl, feeling)
        }
    )
}

@Composable
fun CommentsDialog(
    post: Post,
    currentUser: User? = null,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: ((Long) -> Unit)? = null
) {
    var commentText by remember { mutableStateOf("") }
    var selectedCommentForActions by remember { mutableStateOf<Comment?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .testTag("comments_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments (${post.comments.size})",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = AppColors.textPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AppColors.textSecondary
                        )
                    }
                }

                HorizontalDivider(color = AppColors.border)

                // Comments List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (post.comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Be the first to comment!",
                                    color = AppColors.textSecondary,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(post.comments, key = { it.id }) { comment ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedCommentForActions = comment }
                            ) {
                                AsyncImage(
                                    model = comment.author.profileImageUrl,
                                    contentDescription = comment.author.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.bgInput)
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = comment.author.name,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = AppColors.textPrimary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.MoreHoriz,
                                            contentDescription = "Options",
                                            tint = AppColors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = comment.text,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = AppColors.textBody
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment.timestamp,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 10.sp,
                                        color = AppColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = AppColors.border)

                // Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp
                        ),
                        placeholder = { Text("Write a comment…", color = AppColors.textMuted, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_comment_input"),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppColors.bgInput,
                            unfocusedContainerColor = AppColors.bgInput,
                            focusedTextColor = AppColors.textPrimary,
                            unfocusedTextColor = AppColors.textPrimary,
                            focusedBorderColor = AppColors.primaryBlue,
                            unfocusedBorderColor = AppColors.border
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AppColors.primaryBlue)
                            .testTag("submit_comment_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Comment",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Comment Action Modal for comments
    selectedCommentForActions?.let { c ->
        CommentActionModal(
            isOpen = true,
            onClose = { selectedCommentForActions = null },
            comment = c,
            currentUserId = currentUser?.id ?: 0L,
            isAdmin = currentUser?.role in listOf("admin", "moderator"),
            onDelete = { commentId ->
                onDeleteComment?.invoke(commentId)
            },
            onReply = { comment ->
                commentText = "@${comment.author.name} "
            }
        )
    }
}

@Composable
fun ProductDetailDialog(
    product: Product,
    onDismiss: () -> Unit,
    onContactSeller: (User) -> Unit
) {
    val allImages = if (product.images.isNotEmpty()) product.images else listOf(product.imageUrl)
    var activeImageIndex by remember { mutableStateOf(0) }
    val displayPrice = product.discountPrice ?: product.price
    val formattedPrice = formatMarketPrice(displayPrice, product.country)

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CoordinatedDestinationReveal(
            destinationType = DestinationType.MARKETPLACE_ITEM,
            key = product.id
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.bgScreen)
                .testTag("product_detail_dialog")
        ) {
            // Header
            Surface(
                color = AppColors.headerBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.headerIconTint
                        )
                    }

                    Text(
                        text = "MarketPoint",
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.headerText
                    )

                    IconButton(
                        onClick = { /* More options */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = AppColors.headerIconTint.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Square Image Carousel
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(AppColors.bgCard)
                    ) {
                        val currentImageUrl = allImages.getOrElse(activeImageIndex) { product.imageUrl }
                        AsyncImage(
                            model = currentImageUrl,
                            contentDescription = product.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // "Ad" badge if sponsored
                        if (product.isSponsored) {
                            Surface(
                                color = Color(0xFFF97316),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Ad",
                                    fontSize = 11.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Prev / Next Arrows if multiple images
                        if (allImages.size > 1) {
                            IconButton(
                                onClick = {
                                    activeImageIndex = if (activeImageIndex > 0) activeImageIndex - 1 else allImages.size - 1
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(8.dp)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    activeImageIndex = if (activeImageIndex < allImages.size - 1) activeImageIndex + 1 else 0
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(8.dp)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Thumbnail strip if multiple images
                if (allImages.size > 1) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.bgSurface)
                        ) {
                            items(allImages.indices.toList()) { idx ->
                                val isSelected = activeImageIndex == idx
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            2.dp,
                                            if (isSelected) AppColors.primaryBlue else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { activeImageIndex = idx }
                                ) {
                                    AsyncImage(
                                        model = allImages[idx],
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // Title and Pricing Section matching web
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.bgSurface)
                            .padding(16.dp)
                    ) {
                        if (product.isSponsored) {
                            Surface(
                                color = Color(0xFFF97316).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.4f)),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "Ad • Sponsored",
                                    fontSize = 11.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (AppColors.isLight) Color(0xFFEA580C) else Color(0xFFFB923C),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = product.title,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            color = AppColors.textPrimary,
                            lineHeight = 30.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = formattedPrice,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 28.sp,
                            color = AppColors.textPrimary
                        )

                        if (product.discountPrice != null) {
                            Text(
                                text = formatMarketPrice(product.price, product.country),
                                fontSize = 16.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = AppColors.textSecondary,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = InterFontFamily,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = AppColors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = product.location,
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = AppColors.textSecondary
                            )
                        }
                    }
                }

                // Seller Card Section matching web
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = AppColors.bgSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = product.seller.profileImageUrl,
                                contentDescription = product.seller.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.bgInput)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = product.seller.name,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = AppColors.textPrimary
                                    )
                                    if (product.seller.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerifiedBadge(size = 16.dp)
                                    }
                                }
                                Text(
                                    text = "Seller",
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.textSecondary
                                )
                            }

                            // Message Button
                            IconButton(
                                onClick = { onContactSeller(product.seller) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.primaryBlue.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Message",
                                    tint = AppColors.primaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Phone Call Button
                            if (product.phoneNumber.isNotBlank()) {
                                IconButton(
                                    onClick = { /* Call action */ },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.bgInput)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = AppColors.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Description Section matching web
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.bgSurface)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Description",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = AppColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.description.ifBlank { "No description provided for this item." },
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = AppColors.textSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Bottom Sticky "Contact Seller" Button matching web
            Surface(
                color = AppColors.bgSurface,
                shadowElevation = if (AppColors.isLight) 4.dp else 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onContactSeller(product.seller) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp)
                        .testTag("contact_seller_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Message Seller",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
        }
    }
}

@Composable
fun CreateProductDialog(
    currentUser: User? = null,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        price: Double,
        discountPrice: Double?,
        category: String,
        location: String,
        country: String,
        description: String,
        imageUrl: String,
        images: List<String>,
        phone: String
    ) -> Unit
) {
    val userCountry = remember(currentUser?.location) { resolveUserCountry(currentUser?.location) }
    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var discountPriceText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("electronics") }
    var location by remember { mutableStateOf(currentUser?.location?.ifBlank { "Dar es Salaam, Tanzania" } ?: "Dar es Salaam, Tanzania") }
    var countryCode by remember { mutableStateOf(userCountry.code) }
    var description by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+255 712 345 678") }
    var imageUrlInput by remember { mutableStateOf("") }
    var imagesList by remember { mutableStateOf(listOf<String>()) }
    var isPublishing by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            imagesList = (imagesList + uris.map { it.toString() }).take(10)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.bgScreen)
                .testTag("create_product_dialog")
        ) {
            // Header
            Surface(
                color = AppColors.headerBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.headerIconTint
                        )
                    }

                    Text(
                        text = "New listing",
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.headerText
                    )

                    TextButton(
                        onClick = {
                            if (title.isNotBlank() && priceText.isNotBlank()) {
                                isPublishing = true
                                val priceVal = priceText.toDoubleOrNull() ?: 0.0
                                val discountVal = discountPriceText.toDoubleOrNull()
                                val finalImages = if (imagesList.isNotEmpty()) imagesList else listOf(imageUrlInput.ifBlank { "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=800" })
                                onSubmit(
                                    title,
                                    priceVal,
                                    discountVal,
                                    category,
                                    location,
                                    countryCode,
                                    description,
                                    finalImages.first(),
                                    finalImages,
                                    phone
                                )
                            }
                        },
                        enabled = !isPublishing
                    ) {
                        Text(
                            text = if (isPublishing) "Publishing…" else "Publish",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = if (AppColors.isLight) Color.White else AppColors.primaryBlue
                        )
                    }
                }
            }

            // Scrollable Form matching web
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photos Section matching web
                item {
                    Column {
                        Text(
                            text = "Photos",
                            fontSize = 16.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Add photo card
                        Surface(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = AppColors.bgCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = AppColors.textSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add photos",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = AppColors.textPrimary
                                )
                                Text(
                                    text = "Up to 10 images",
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.textSecondary
                                )
                            }
                        }

                        // Uploaded previews grid
                        if (imagesList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(imagesList.indices.toList()) { idx ->
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = imagesList[idx],
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = {
                                                imagesList = imagesList.filterIndexed { i, _ -> i != idx }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Category selector matching web
                item {
                    Column {
                        Text(
                            text = "Category",
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(WEB_MARKETPLACE_CATEGORIES.filter { it.id != "all" }) { cat ->
                                val isSelected = category.equals(cat.id, ignoreCase = true)
                                Surface(
                                    onClick = { category = cat.id },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) AppColors.primaryBlue else AppColors.bgCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AppColors.primaryBlue else AppColors.border),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    ) {
                                        Text(
                                            text = cat.name,
                                            fontSize = 13.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) Color.White else AppColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Title input: "What are you selling?"
                item {
                    Column {
                        Text(
                            text = "What are you selling?",
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal
                            ),
                            placeholder = { Text("Title", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_title_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppColors.bgCard,
                                unfocusedContainerColor = AppColors.bgCard,
                                focusedBorderColor = AppColors.primaryBlue,
                                unfocusedBorderColor = AppColors.border,
                                focusedTextColor = AppColors.textPrimary,
                                unfocusedTextColor = AppColors.textPrimary
                            ),
                            singleLine = true
                        )
                    }
                }

                // Price input with currency prefix matching web
                item {
                    Column {
                        Text(
                            text = "Price",
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.bgCard)
                                .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getCurrencyLabel(countryCode),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.textPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            VerticalDivider(color = AppColors.border)
                            TextField(
                                value = priceText,
                                onValueChange = { priceText = it },
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal
                                ),
                                placeholder = { Text("Price", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("product_price_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary
                                ),
                                singleLine = true
                            )
                        }
                    }
                }

                // Discount price input matching web
                item {
                    Column {
                        Text(
                            text = "Discount price (optional)",
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.bgCard)
                                .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getCurrencyLabel(countryCode),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.textPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            VerticalDivider(color = AppColors.border)
                            TextField(
                                value = discountPriceText,
                                onValueChange = { discountPriceText = it },
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal
                                ),
                                placeholder = { Text("Discount price", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                modifier = Modifier.fillMaxSize(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary
                                ),
                                singleLine = true
                            )
                        }
                    }
                }

                // Location input matching web
                item {
                    Column {
                        Text(
                            text = "Location",
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal
                            ),
                            placeholder = { Text("City, street or region…", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = AppColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppColors.bgCard,
                                unfocusedContainerColor = AppColors.bgCard,
                                focusedBorderColor = AppColors.primaryBlue,
                                unfocusedBorderColor = AppColors.border,
                                focusedTextColor = AppColors.textPrimary,
                                unfocusedTextColor = AppColors.textPrimary
                            ),
                            singleLine = true
                        )
                    }
                }

                // Country selector: Auto-detected from user profile, hiding all other countries
                item {
                    Column {
                        Text(
                            text = "Country (from your profile)",
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppColors.bgCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(text = userCountry.flag, fontSize = 20.sp, fontFamily = InterFontFamily)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = userCountry.name,
                                    fontSize = 14.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AppColors.primaryBlue.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Auto-detected • ${getCurrencyLabel(userCountry.code)}",
                                        fontSize = 11.sp,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Your listing is automatically priced in ${getCurrencyLabel(userCountry.code)} and displayed to buyers across ${userCountry.name}.",
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = AppColors.textMuted,
                            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }
                }

                // Description input matching web
                item {
                    Column {
                        Text(
                            text = "Description",
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal
                            ),
                            placeholder = { Text("Describe what you are selling…", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppColors.bgCard,
                                unfocusedContainerColor = AppColors.bgCard,
                                focusedBorderColor = AppColors.primaryBlue,
                                unfocusedBorderColor = AppColors.border,
                                focusedTextColor = AppColors.textPrimary,
                                unfocusedTextColor = AppColors.textPrimary
                            )
                        )
                    }
                }

                // Phone input matching web
                item {
                    Column {
                        Text(
                            text = "Phone Number",
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal
                            ),
                            placeholder = { Text("Phone number", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = AppColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppColors.bgCard,
                                unfocusedContainerColor = AppColors.bgCard,
                                focusedBorderColor = AppColors.primaryBlue,
                                unfocusedBorderColor = AppColors.border,
                                focusedTextColor = AppColors.textPrimary,
                                unfocusedTextColor = AppColors.textPrimary
                            ),
                            singleLine = true
                        )
                    }
                }

                // Submit button matching web
                item {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && priceText.isNotBlank()) {
                                isPublishing = true
                                val priceVal = priceText.toDoubleOrNull() ?: 0.0
                                val discountVal = discountPriceText.toDoubleOrNull()
                                val finalImages = if (imagesList.isNotEmpty()) imagesList else listOf(imageUrlInput.ifBlank { "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=800" })
                                onSubmit(
                                    title,
                                    priceVal,
                                    discountVal,
                                    category,
                                    location,
                                    countryCode,
                                    description,
                                    finalImages.first(),
                                    finalImages,
                                    phone
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_product_btn")
                    ) {
                        Text(
                            text = if (isPublishing) "Publishing…" else "Publish",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}


@Composable
fun CreateEventDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, date: String, location: String, description: String, imageUrl: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("create_event_dialog"),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Create an Event",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = AppColors.textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                    placeholder = { Text("Event Name", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.bgInput,
                        unfocusedContainerColor = AppColors.bgInput,
                        focusedBorderColor = AppColors.primaryBlue,
                        unfocusedBorderColor = AppColors.border,
                        focusedTextColor = AppColors.textPrimary,
                        unfocusedTextColor = AppColors.textPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                    placeholder = { Text("Date & Time (e.g. Nov 20 • 6 PM)", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.bgInput,
                        unfocusedContainerColor = AppColors.bgInput,
                        focusedBorderColor = AppColors.primaryBlue,
                        unfocusedBorderColor = AppColors.border,
                        focusedTextColor = AppColors.textPrimary,
                        unfocusedTextColor = AppColors.textPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                    placeholder = { Text("Location / Venue", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.bgInput,
                        unfocusedContainerColor = AppColors.bgInput,
                        focusedBorderColor = AppColors.primaryBlue,
                        unfocusedBorderColor = AppColors.border,
                        focusedTextColor = AppColors.textPrimary,
                        unfocusedTextColor = AppColors.textPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                    placeholder = { Text("Event Description", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.bgInput,
                        unfocusedContainerColor = AppColors.bgInput,
                        focusedBorderColor = AppColors.primaryBlue,
                        unfocusedBorderColor = AppColors.border,
                        focusedTextColor = AppColors.textPrimary,
                        unfocusedTextColor = AppColors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSubmit(
                            title,
                            date.ifBlank { "Next Weekend" },
                            location.ifBlank { "Dar es Salaam, Tanzania" },
                            description,
                            imageUrl
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_event_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Publish Event",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    posts: List<Post>,
    products: List<Product>,
    users: List<User>,
    onSelectUser: (User) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredPosts = if (query.isBlank()) emptyList() else posts.filter {
        it.content.contains(query, ignoreCase = true)
    }
    val filteredProducts = if (query.isBlank()) emptyList() else products.filter {
        it.title.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
    }
    val filteredUsers = if (query.isBlank()) emptyList() else users.filter {
        it.name.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .testTag("search_dialog"),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Input Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp
                        ),
                        placeholder = { Text("Search Feathered…", color = AppColors.textMuted, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = AppColors.primaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = AppColors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppColors.bgInput,
                            unfocusedContainerColor = AppColors.bgInput,
                            focusedBorderColor = AppColors.primaryBlue,
                            unfocusedBorderColor = AppColors.border,
                            focusedTextColor = AppColors.textPrimary,
                            unfocusedTextColor = AppColors.textPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("global_search_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AppColors.textSecondary
                        )
                    }
                }

                HorizontalDivider(color = AppColors.border)

                // Results
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (query.isBlank()) {
                        item {
                            Text(
                                text = "Search for friends, topics, products or events.",
                                color = AppColors.textSecondary,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        // People
                        if (filteredUsers.isNotEmpty()) {
                            item {
                                Text(
                                    text = "PEOPLE",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = AppColors.primaryBlue
                                )
                            }
                            items(filteredUsers) { u ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelectUser(u)
                                            onDismiss()
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = u.profileImageUrl,
                                        contentDescription = u.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(u.name, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppColors.textPrimary)
                                        Text("@${u.username}", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = AppColors.textSecondary)
                                    }
                                }
                            }
                        }

                        // Products
                        if (filteredProducts.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "MARKETPLACE",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = AppColors.primaryBlue
                                )
                            }
                            items(filteredProducts) { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = p.imageUrl,
                                        contentDescription = p.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(p.title, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AppColors.textPrimary)
                                        Text("${p.currency} ${p.price}", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = AppColors.primaryBlue)
                                    }
                                }
                            }
                        }

                        // Posts
                        if (filteredPosts.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "POSTS",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = AppColors.primaryBlue
                                )
                            }
                            items(filteredPosts) { pst ->
                                Text(
                                    text = pst.content,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = AppColors.textPrimary,
                                    maxLines = 2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AppColors.bgInput, RoundedCornerShape(8.dp))
                                        .border(1.dp, AppColors.border, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
