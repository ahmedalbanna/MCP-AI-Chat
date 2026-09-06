package com.aistudio.mcpchat.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aistudio.mcpchat.data.local.ChatMessageEntity
import com.aistudio.mcpchat.data.model.ImageAttachment
import com.aistudio.mcpchat.data.model.parseImageAttachments
import com.aistudio.mcpchat.ui.theme.AmberWarning
import com.aistudio.mcpchat.ui.theme.CyanSecondary
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.EmeraldTertiary
import com.aistudio.mcpchat.ui.theme.IndigoPrimary
import com.aistudio.mcpchat.ui.theme.IndigoPrimaryLight
import com.aistudio.mcpchat.ui.theme.RoseError
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.ChatViewModel
import com.aistudio.mcpchat.ui.viewmodel.StreamProgress
import com.aistudio.mcpchat.ui.viewmodel.formatRemainingMs
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

    val currentSession = sessions.find { it.id == currentSessionId }
    val enabledServersCount = servers.count { it.isEnabled }
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

    // Key on the LAST message identity, not the list size: growing the
    // history window prepends items and must not yank the user to the
    // bottom, while a genuinely new message should.
    LaunchedEffect(messages.lastOrNull()?.id, inProgressToolCalls.size, streamingText?.length) {
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MCP AI Chat",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Surface(
                                onClick = onNavigateToProviders,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(Dimens.cornerRadiusSm),
                                border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
                                    .background(EmeraldTertiary),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = "$enabledServersCount Remote MCP Servers Active",
                                fontSize = 11.sp,
                                color = EmeraldTertiary,
                                fontFamily = FontFamily.Monospace,
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
            .imePadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty() && inProgressToolCalls.isEmpty()) {
                    EmptyChatView(
                        onPromptSelected = { prompt ->
                            viewModel.sendMessage(prompt)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chat_messages_list"),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageItem(
                                message = msg,
                                viewModel = viewModel,
                            )
                            if (msg.id == lastMessageId && !isGenerating) {
                                RegenerateAction(
                                    onClick = { viewModel.regenerate() },
                                    modifier = Modifier.padding(start = Spacing.xxl),
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
                                            color = AmberWarning,
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.sm))
                                        Text(
                                            text = "Executing Remote MCP Tool Calls...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AmberWarning,
                                            fontFamily = FontFamily.Monospace,
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
                    color = RoseError.copy(alpha = 0.15f),
                    border = BorderStroke(Dimens.borderWidth, RoseError.copy(alpha = 0.4f)),
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
                            contentDescription = "Error",
                            tint = RoseError,
                            modifier = Modifier.size(Dimens.iconSizeSm),
                        )
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = RoseError,
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
                                tint = RoseError,
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
                tonalElevation = Dimens.elevationMd,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
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
                        // Gradient fade hint — only when chips can scroll forward
                        if (chipsScrollState.canScrollForward) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(24.dp)
                                    .height(32.dp)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surface,
                                            ),
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
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = { submitPrompt() },
                            ),
                            shape = RoundedCornerShape(Dimens.cornerRadiusXl),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                                        isGenerating -> RoseError
                                        canSend -> IndigoPrimary
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Toggle servers enabled for AI tool discovery",
                            fontSize = 12.sp,
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
                    items(servers) { server ->
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
                                            fontSize = 14.sp,
                                        )
                                        if (server.isPreset) {
                                            Spacer(modifier = Modifier.width(Spacing.sm))
                                            Surface(
                                                color = CyanSecondary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(Dimens.cornerRadiusSm),
                                            ) {
                                                Text(
                                                    text = "PRESET",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyanSecondary,
                                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${server.toolsCount} tools • ${server.endpointUrl}",
                                        fontSize = 11.sp,
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
                                        checkedThumbColor = IndigoPrimary,
                                        checkedTrackColor = IndigoPrimary.copy(alpha = 0.3f),
                                    ),
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
                        fontSize = 18.sp,
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
                        Text(text = "Search conversations and messages…", fontSize = 14.sp)
                    },
                    singleLine = true,
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Spacing.xxs),
                            )
                        }
                        if (searchResults.isEmpty()) {
                            item {
                                Text(
                                    text = "No messages matched \"$searchQuery\".",
                                    fontSize = 13.sp,
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xxs),
                                )
                            }
                        }
                    }
                    items(visibleSessions) { session ->
                        val isSelected = session.id == currentSessionId
                        Surface(
                            color = sessionSurfaceColor(isSelected),
                            shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                            border = if (isSelected) BorderStroke(Dimens.borderWidth, IndigoPrimary) else null,
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
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        text = sessionDateFormatter.format(Date(session.updatedAt)),
                                        fontSize = 11.sp,
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
                                        tint = RoseError.copy(alpha = 0.7f),
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
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RoseError),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(message: ChatMessageEntity, viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val shouldTruncate = !isUser && message.content.length > 500

    val toolCalls = remember(message.toolCallsJson) {
        if (message.toolCallsJson != null) {
            viewModel.chatRepository.parseToolCallsJson(message.toolCallsJson)
        } else {
            emptyList()
        }
    }

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        if (!isUser && toolCalls.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(bottom = Spacing.sm),
            ) {
                Text(
                    text = "MCP Invocations (${toolCalls.size}):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = Spacing.xxs),
                )
                for (call in toolCalls) {
                    ToolCallCard(toolCall = call)
                }
            }
        }

        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!isUser) {
                Surface(
                    color = IndigoPrimary.copy(alpha = 0.2f),
                    shape = CircleShape,
                    border = BorderStroke(Dimens.borderWidth, IndigoPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(Spacing.xxl)
                        .padding(top = Spacing.xxs),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = IndigoPrimaryLight,
                            modifier = Modifier.size(Dimens.iconSizeSm),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
            }

            Surface(
                color = if (isUser) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (!isUser) Dimens.elevationSm else 0.dp,
                shape = RoundedCornerShape(
                    topStart = Dimens.cornerRadiusXl,
                    topEnd = Dimens.cornerRadiusXl,
                    bottomStart = if (isUser) Dimens.cornerRadiusXl else Dimens.cornerRadiusSm,
                    bottomEnd = if (isUser) Dimens.cornerRadiusSm else Dimens.cornerRadiusXl,
                ),
                border = if (isUser) null else BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth(Dimens.CHAT_BUBBLE_MAX_WIDTH)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = {},
                        onLongClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Message", message.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                        },
                    ),
            ) {
                Column(modifier = Modifier.padding(horizontal = Spacing.cardPadding, vertical = Spacing.sm)) {
                    if (isUser) {
                        val attachments = remember(message.attachmentsJson) {
                            parseImageAttachments(message.attachmentsJson)
                        }
                        for (attachment in attachments) {
                            AsyncImage(
                                model = attachment.uri,
                                contentDescription = "Attached image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(Dimens.cornerRadiusMd))
                                    .padding(bottom = Spacing.xs)
                                    .testTag("message_attachment_image"),
                            )
                        }
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            )
                        }
                    } else {
                        val displayText = if (shouldTruncate && !isExpanded) {
                            message.content.take(500) + "..."
                        } else {
                            message.content
                        }
                        MarkdownFormattedText(text = displayText)

                        if (shouldTruncate) {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = if (isExpanded) "Show less" else "Show more",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanSecondary,
                                modifier = Modifier.clickable { isExpanded = !isExpanded },
                            )
                        }
                    }

                    if (message.latencyMs > 0 && !isUser) {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.xs),
                        ) {
                            Text(
                                text = "${message.latencyMs}ms",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatView(onPromptSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xl),
    ) {
        Surface(
            color = IndigoPrimary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(Dimens.cornerRadiusXl),
            border = BorderStroke(Dimens.borderWidth, IndigoPrimary.copy(alpha = 0.3f)),
            modifier = Modifier.size(Spacing.xxxl),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = IndigoPrimaryLight,
                    modifier = Modifier
                        .size(Dimens.iconSizeXl)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        },
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "Welcome to MCP AI Chat",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = "Connected to remote MCP servers. Seamless tool calling over JSON-RPC & HTTP SSE.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FeatureHighlightItem(
                icon = Icons.Default.AutoAwesome,
                label = "Chat with AI",
                modifier = Modifier.weight(1f),
            )
            FeatureHighlightItem(
                icon = Icons.Default.Hub,
                label = "MCP Tool Calling",
                modifier = Modifier.weight(1f),
            )
            FeatureHighlightItem(
                icon = Icons.Default.Speed,
                label = "Inspector",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            text = "TRY ASKING:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyanSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start),
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PromptSuggestionCard(
                icon = Icons.Default.WbSunny,
                title = "Live Weather Telemetry",
                prompt = "Check current temperature, humidity, and forecast for London and Tokyo using MCP weather tools.",
                onPromptSelected = onPromptSelected,
            )
            PromptSuggestionCard(
                icon = Icons.Default.Code,
                title = "GitHub Code Explorer",
                prompt = "Search GitHub for the top starred Kotlin Multiplatform and Compose MCP repositories.",
                onPromptSelected = onPromptSelected,
            )
            PromptSuggestionCard(
                icon = Icons.Default.CurrencyBitcoin,
                title = "Crypto & Forex Markets",
                prompt = "Get current Bitcoin and Solana prices, and convert 5000 EUR to USD.",
                onPromptSelected = onPromptSelected,
            )
            PromptSuggestionCard(
                icon = Icons.Default.Schedule,
                title = "World Time & Timezones",
                prompt = "What is the exact current time in Sydney, Tokyo, and San Francisco?",
                onPromptSelected = onPromptSelected,
            )
        }
    }
}

@Composable
private fun FeatureHighlightItem(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = Spacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IndigoPrimaryLight,
                modifier = Modifier.size(Dimens.iconSizeLg),
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromptSuggestionCard(icon: ImageVector, title: String, prompt: String, onPromptSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "Send prompt: $title"
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onPromptSelected(prompt) },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Spacing.md),
        ) {
            Surface(
                color = IndigoPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(Dimens.cornerRadiusMd),
                modifier = Modifier.size(Dimens.iconSizeXl),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = IndigoPrimaryLight,
                        modifier = Modifier.size(Dimens.iconSizeMd),
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = prompt,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickActionChip(label: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(Dimens.cornerRadiusXl),
        border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onClick() },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = IndigoPrimaryLight,
                    modifier = Modifier.size(Dimens.iconSizeSm),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(Dimens.cornerRadiusXl, Dimens.cornerRadiusXl, Dimens.cornerRadiusXl, Dimens.cornerRadiusSm),
            )
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.sm),
    ) {
        Surface(
            color = IndigoPrimary.copy(alpha = 0.2f),
            shape = CircleShape,
            border = BorderStroke(Dimens.borderWidth, IndigoPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.size(Spacing.xxl),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI is typing",
                    tint = IndigoPrimaryLight,
                    modifier = Modifier.size(Dimens.iconSizeSm),
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        PulsingDots()
    }
}

/** Three pulsing dots signalling liveness; usable bare or inside a bubble. */
@Composable
fun PulsingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "typingAlpha",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier,
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(Spacing.sm)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
private fun sessionSurfaceColor(isSelected: Boolean): Color = if (isSelected) {
    IndigoPrimary.copy(alpha = 0.15f)
} else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
}

/**
 * One full-text search hit: content snippet, originating conversation, and
 * timestamp. Tapping jumps to that conversation.
 */
@Composable
fun MessageSearchResultItem(
    message: ChatMessageEntity,
    sessionTitle: String?,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = sessionSurfaceColor(false),
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        modifier = modifier
            .fillMaxWidth()
            .testTag("message_search_result")
            .semantics {
                role = Role.Button
                contentDescription = "Open conversation: ${sessionTitle ?: "Conversation"}"
            }
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = message.content.take(120).ifBlank { "(no text)" },
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "${sessionTitle ?: "Conversation"} • ${dateFormatter.format(Date(message.timestamp))}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Live preview bubble for the assistant reply while tokens stream in over
 * SSE. Mirrors the assistant bubble styling; replaced by the persisted
 * message (via the Room messages flow) once the stream finishes.
 */
@Composable
fun StreamingMessageBubble(text: String, progress: StreamProgress? = null, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Surface(
            color = IndigoPrimary.copy(alpha = 0.2f),
            shape = CircleShape,
            border = BorderStroke(Dimens.borderWidth, IndigoPrimary.copy(alpha = 0.4f)),
            modifier = Modifier
                .size(Spacing.xxl)
                .padding(top = Spacing.xxs),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = IndigoPrimaryLight,
                    modifier = Modifier.size(Dimens.iconSizeSm),
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.sm))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = Dimens.elevationSm,
            shape = RoundedCornerShape(
                topStart = Dimens.cornerRadiusXl,
                topEnd = Dimens.cornerRadiusXl,
                bottomStart = Dimens.cornerRadiusSm,
                bottomEnd = Dimens.cornerRadiusXl,
            ),
            border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth(Dimens.CHAT_BUBBLE_MAX_WIDTH),
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.cardPadding, vertical = Spacing.sm)) {
                MarkdownFormattedText(text = text)
                StreamProgressIndicator(
                    progress = progress,
                    modifier = Modifier.padding(top = Spacing.xxs),
                )
            }
        }
    }
}

/**
 * Determinate stream progress bar with throughput and remaining-time
 * estimates; indeterminate (with a waiting label) until the first token
 * arrives. [progress] snapshots come from the ViewModel's tracker.
 */
@Composable
fun StreamProgressIndicator(progress: StreamProgress?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (progress == null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(Dimens.borderWidthThick),
            )
        } else {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(Dimens.borderWidthThick),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
            text = if (progress == null) {
                "Waiting for first token…"
            } else {
                "${progress.tokensPerSecond} tok/s · ${progress.charsStreamed} chars · ~${formatRemainingMs(progress.estimatedRemainingMs)} left"
            },
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
            },
        )
    }
}

/**
 * Re-runs generation for the last user turn: replaces the previous assistant
 * reply, a stopped partial, or an error message.
 */
@Composable
fun RegenerateAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(Dimens.cornerRadiusMd),
        border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        onClick = onClick,
        modifier = modifier
            .testTag("regenerate_action")
            .semantics { role = Role.Button },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = CyanSecondary,
                modifier = Modifier.size(Dimens.iconSizeSm),
            )
            Text(
                text = "Regenerate",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanSecondary,
            )
        }
    }
}

/**
 * Thumbnails of images staged in the input bar, plus a trailing tile that
 * reopens the photo picker.
 */
@Composable
fun PendingAttachmentsBar(
    attachments: List<ImageAttachment>,
    onRemove: (ImageAttachment) -> Unit,
    onAddMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier.testTag("pending_attachments_bar"),
    ) {
        items(attachments, key = { it.uri }) { attachment ->
            Box {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = "Attached image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(Dimens.cornerRadiusSm)),
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xxs),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = RoseError,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onRemove(attachment) }
                            .testTag("remove_attachment_button"),
                    )
                }
            }
        }
        item(key = "add_attachment_tile") {
            IconButton(
                onClick = onAddMore,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(Dimens.cornerRadiusSm))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("add_attachment_tile"),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add another image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.iconSizeSm),
                )
            }
        }
    }
}
