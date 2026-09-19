package com.aistudio.mcpchat.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    providerViewModel: com.aistudio.mcpchat.ui.viewmodel.AiProviderSettingsViewModel,
    onNavigateToServers: () -> Unit,
    onNavigateToProviders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val inProgressToolCalls by viewModel.inProgressToolCalls.collectAsStateWithLifecycle()
    val hasPendingUserTurn by viewModel.hasPendingUserTurn.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val streamProgress by viewModel.streamProgress.collectAsStateWithLifecycle()
    val pendingAttachments by viewModel.pendingAttachments.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showServersSheet by remember { mutableStateOf(false) }
    var showSessionsSheet by remember { mutableStateOf(false) }
    val providerUiState by providerViewModel.uiState.collectAsStateWithLifecycle()
    val activeProviderConfig = providerUiState.configs.find { it.id == providerUiState.activeId }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Photo picker (backported on older devices by the contract itself).
    // The persistable grant keeps the URI readable after a reboot; some
    // fallback intents do not offer it, hence the runCatching.
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val mimeType = context.contentResolver.getType(uri) ?: "image/*"
            viewModel.addPendingAttachment(uri.toString(), mimeType)
        }
    }
    val launchPhotoPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val currentSession by remember(sessions, currentSessionId) {
        derivedStateOf { sessions.find { it.id == currentSessionId } }
    }
    val enabledServersCount by remember(servers) {
        derivedStateOf { servers.count { it.isEnabled } }
    }
    val lastMessageId = messages.lastOrNull()?.id

    // Keep the session's model compatible whenever the active provider changes
    // (Google providers need gemini-* models, the others non-gemini ones).
    LaunchedEffect(providerUiState.activeId, currentSessionId) {
        val config = providerUiState.configs.find { it.id == providerUiState.activeId }
            ?: return@LaunchedEffect
        val session = currentSession ?: return@LaunchedEffect
        val compatible = when (config.type) {
            com.aistudio.mcpchat.data.model.AiProviderType.GOOGLE -> session.model.startsWith("gemini")
            else -> !session.model.startsWith("gemini")
        }
        if (!compatible) {
            viewModel.updateSessionModel(config.model)
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val chipsScrollState = rememberScrollState()

    var userHasScrolledUp by remember { mutableStateOf(false) }

    val isStreaming = streamingText != null
    val isEmptyChat by remember(messages, inProgressToolCalls, isStreaming, isGenerating) {
        derivedStateOf {
            messages.isEmpty() && inProgressToolCalls.isEmpty() && !isStreaming && !isGenerating
        }
    }
    LaunchedEffect(messages.lastOrNull()?.id, inProgressToolCalls.size, isStreaming) {
        if (!userHasScrolledUp && messages.isNotEmpty()) {
            val count = listState.layoutInfo.totalItemsCount
            if (count > 0) listState.scrollToItem(count - 1)
        }
    }

    @OptIn(FlowPreview::class)
    LaunchedEffect(listState) {
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0,
                listState.layoutInfo.totalItemsCount,
            )
        }.debounce(100).collect { (firstVisible, lastVisible, total) ->
            userHasScrolledUp = total > 0 && lastVisible < total - 3 && firstVisible > 0
            // Pinned at the very top — back-fill one page of history if the
            // window is full (loadEarlierMessages no-ops at the list start).
            if (firstVisible == 0 && total > 0) {
                viewModel.loadEarlierMessages()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MCP AI Chat",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Surface(
                                onClick = onNavigateToProviders,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(Dimens.cornerRadiusFull),
                                border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .semantics {
                                        contentDescription = "Select AI provider and model"
                                        stateDescription = "${activeProviderConfig?.name ?: "No provider"}: ${currentSession?.model ?: ""}"
                                    }
                                    .testTag("model_picker_chip"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                                ) {
                                    Text(
                                        text = if (activeProviderConfig != null) {
                                            "${activeProviderConfig.name}: ${currentSession?.model ?: activeProviderConfig.model}"
                                        } else {
                                            "Select provider"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(Dimens.iconSizeSm),
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "View active MCP servers"
                                }
                                .clickable { showServersSheet = true },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(Spacing.xs)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = "$enabledServersCount servers active",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSessionsSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("chat_history_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Chat History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = { viewModel.createNewChat() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("new_chat_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (isEmptyChat) {
                    EmptyChatView(
                        onPromptSelected = { prompt ->
                            viewModel.sendMessage(prompt)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chat_messages_list"),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageItem(
                                message = msg,
                                viewModel = viewModel,
                            )
                            if (msg.id == lastMessageId && !isGenerating && msg.role != "user") {
                                RegenerateAction(
                                    onClick = { viewModel.regenerate() },
                                    modifier = Modifier.padding(top = Spacing.xs),
                                )
                            }
                        }

                        if (inProgressToolCalls.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = Spacing.md, end = Spacing.xxl),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = Spacing.sm),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(Dimens.iconSizeSm),
                                            strokeWidth = Dimens.borderWidthThick,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.sm))
                                        Text(
                                            text = "Using tools…",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    for (call in inProgressToolCalls) {
                                        ToolCallCard(toolCall = call)
                                    }
                                }
                            }
                        }

                        if (streamingText != null) {
                            item(key = "streaming_bubble") {
                                StreamingMessageBubble(
                                    text = streamingText.orEmpty(),
                                    progress = streamProgress,
                                    modifier = Modifier.padding(start = Spacing.md, end = Spacing.xxl),
                                )
                            }
                        }

                        // Progress bar placeholder between send and first token.
                        val awaitingFirstToken = isGenerating &&
                            inProgressToolCalls.isEmpty() &&
                            hasPendingUserTurn &&
                            streamingText == null
                        if (awaitingFirstToken) {
                            item(key = "stream_progress") {
                                StreamProgressIndicator(
                                    progress = null,
                                    modifier = Modifier.padding(start = Spacing.md, end = Spacing.xxl),
                                )
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = userHasScrolledUp,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = Spacing.lg, bottom = Spacing.sm),
                ) {
                    Surface(
                        onClick = {
                            userHasScrolledUp = false
                            coroutineScope.launch {
                                listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = Dimens.elevationLg,
                        modifier = Modifier.size(Dimens.touchTargetMin),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Jump to bottom",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(Dimens.iconSizeLg),
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(Dimens.cornerRadiusMd),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                        .semantics {
                            liveRegion = LiveRegionMode.Assertive
                            contentDescription = "Error: ${errorMessage ?: ""}"
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(Dimens.iconSizeSm),
                        )
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(Dimens.iconSizeSm),
                            )
                        }
                    }
                }
            }

            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                thickness = Dimens.borderWidth,
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(chipsScrollState),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            QuickActionChip(
                                label = "Tokyo Weather",
                                icon = Icons.Default.WbSunny,
                            ) {
                                viewModel.sendMessage("Get the live weather in Tokyo and describe current conditions.")
                            }
                            QuickActionChip(
                                label = "Kotlin Repos",
                                icon = Icons.Default.Code,
                            ) {
                                viewModel.sendMessage("Search GitHub for trending Kotlin Jetpack Compose repositories.")
                            }
                            QuickActionChip(
                                label = "BTC & EUR",
                                icon = Icons.Default.CurrencyBitcoin,
                            ) {
                                viewModel.sendMessage("Get the live price of Bitcoin and convert 1500 USD to EUR.")
                            }
                            QuickActionChip(
                                label = "World Time",
                                icon = Icons.Default.Schedule,
                            ) {
                                viewModel.sendMessage("What is the current time in Tokyo, London, and New York?")
                            }
                            QuickActionChip(
                                label = "Math Engine",
                                icon = Icons.Default.Speed,
                            ) {
                                viewModel.sendMessage("Calculate sqrt(1024) + (45 * 12) / 3.5 and convert 150 km to miles.")
                            }
                        }
                        // Gradient fade hint — only when chips can scroll forward.
                        // Mirrored for RTL via LocalLayoutDirection; the overlay
                        // is deliberately non-interactive so chip taps pass through.
                        if (chipsScrollState.canScrollForward) {
                            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                            val fadeColors = if (isRtl) {
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    Color.Transparent,
                                )
                            } else {
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(if (isRtl) Alignment.CenterStart else Alignment.CenterEnd)
                                    .width(Spacing.xl)
                                    .height(Spacing.xxl)
                                    .clickable(enabled = false) { }
                                    .background(
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = fadeColors,
                                        ),
                                    ),
                            )
                        }
                    }

                    val canSend = !isGenerating && (inputText.isNotBlank() || pendingAttachments.isNotEmpty())
                    val submitPrompt = {
                        if (canSend) {
                            val prompt = inputText.trim()
                            inputText = ""
                            keyboardController?.hide()
                            viewModel.sendMessage(prompt)
                        }
                    }

                    if (pendingAttachments.isNotEmpty()) {
                        PendingAttachmentsBar(
                            attachments = pendingAttachments,
                            onRemove = viewModel::removePendingAttachment,
                            onAddMore = launchPhotoPicker,
                            modifier = Modifier.padding(bottom = Spacing.sm),
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IconButton(
                            onClick = launchPhotoPicker,
                            enabled = !isGenerating,
                            modifier = Modifier
                                .size(Dimens.touchTargetMin)
                                .testTag("attach_image_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Attach image",
                                tint = if (isGenerating) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(Dimens.iconSizeLg),
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Ask anything with remote MCP tools...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = { submitPrompt() },
                            ),
                            shape = RoundedCornerShape(Dimens.chatInputRadius),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                        )

                        Spacer(modifier = Modifier.width(Spacing.sm))

                        IconButton(
                            onClick = {
                                if (isGenerating) {
                                    viewModel.cancelGeneration()
                                } else {
                                    submitPrompt()
                                }
                            },
                            enabled = isGenerating || inputText.isNotBlank() || pendingAttachments.isNotEmpty(),
                            modifier = Modifier
                                .size(Dimens.touchTargetMin)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isGenerating -> MaterialTheme.colorScheme.error
                                        canSend -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                )
                                .semantics {
                                    contentDescription = if (isGenerating) "Stop generating" else "Send message"
                                }
                                .testTag("send_message_button"),
                        ) {
                            if (isGenerating) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(Dimens.iconSizeLg),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (canSend) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(Dimens.iconSizeLg),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showServersSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showServersSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sheetPadding),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text(
                            text = "Active MCP Servers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Toggle servers enabled for AI tool discovery",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            showServersSheet = false
                            onNavigateToServers()
                        },
                    ) {
                        Text("Manage Hub")
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(servers, key = { it.id }) { server ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = server.name,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        if (server.isPreset) {
                                            Spacer(modifier = Modifier.width(Spacing.sm))
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(Dimens.cornerRadiusSm),
                                            ) {
                                                Text(
                                                    text = "PRESET",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${server.toolsCount} tools • ${server.endpointUrl}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }

                                Switch(
                                    checked = server.isEnabled,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleServer(server.id, checked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    ),
                                    modifier = Modifier
                                        .semantics {
                                            contentDescription = "${server.name} ${if (server.isEnabled) "enabled" else "disabled"}"
                                            stateDescription = if (server.isEnabled) "On" else "Off"
                                        }
                                        .testTag("toggle_server_${server.id}"),
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }
    }

    if (showSessionsSheet) {
        val sheetState = rememberModalBottomSheetState()
        var sessionToDelete by remember { mutableStateOf<com.aistudio.mcpchat.data.local.ChatSessionEntity?>(null) }
        val sessionDateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
        val searchResults by viewModel.messageSearchResults.collectAsStateWithLifecycle()
        var searchQuery by remember { mutableStateOf("") }
        val isSearching = searchQuery.isNotBlank()
        val titleFilter = searchQuery.trim()
        val visibleSessions = if (titleFilter.isEmpty()) sessions else sessions.filter { it.title.contains(titleFilter, ignoreCase = true) }
        ModalBottomSheet(
            onDismissRequest = {
                showSessionsSheet = false
                searchQuery = ""
                viewModel.updateMessageSearchQuery("")
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sheetPadding),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Conversations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    FilledTonalButton(
                        onClick = {
                            viewModel.createNewChat()
                            showSessionsSheet = false
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Dimens.iconSizeSm))
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("New Chat")
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.updateMessageSearchQuery(it)
                    },
                    placeholder = {
                        Text(text = "Search conversations and messages…", style = MaterialTheme.typography.bodyMedium)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() },
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    shape = RoundedCornerShape(Dimens.cornerRadiusXl),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("message_search_field"),
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSearching) {
                        item {
                            Text(
                                text = "Messages",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Spacing.xxs),
                            )
                        }
                        if (searchResults.isEmpty()) {
                            item {
                                Text(
                                    text = "No messages matched \"$searchQuery\".",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(searchResults, key = { it.id }) { message ->
                            MessageSearchResultItem(
                                message = message,
                                sessionTitle = sessions.find { it.id == message.sessionId }?.title,
                                dateFormatter = sessionDateFormatter,
                                onClick = {
                                    viewModel.selectSession(message.sessionId)
                                    showSessionsSheet = false
                                },
                            )
                        }
                        if (visibleSessions.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Conversations",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xxs),
                                )
                            }
                        }
                    }
                    items(visibleSessions, key = { it.id }) { session ->
                        val isSelected = session.id == currentSessionId
                        Surface(
                            color = sessionSurfaceColor(isSelected),
                            shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                            border = if (isSelected) {
                                BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Switch to ${session.title}"
                                }
                                .clickable {
                                    viewModel.selectSession(session.id)
                                    showSessionsSheet = false
                                },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(Spacing.md),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = sessionDateFormatter.format(Date(session.updatedAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                IconButton(
                                    onClick = { sessionToDelete = session },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete ${session.title}",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(Dimens.iconSizeSm),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xl))
            }
            sessionToDelete?.let { toDelete ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { sessionToDelete = null },
                    title = { androidx.compose.material3.Text("Delete conversation?") },
                    text = { androidx.compose.material3.Text("Delete \"${toDelete.title}\"? This cannot be undone.") },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                viewModel.deleteSession(toDelete.id)
                                sessionToDelete = null
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        ) { androidx.compose.material3.Text("Delete") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { sessionToDelete = null }) { androidx.compose.material3.Text("Cancel") }
                    },
                )
            }
        }
    }
}
