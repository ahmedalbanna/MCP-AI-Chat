package com.aistudio.mcpchat.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.mcpchat.data.local.ThemeMode
import com.aistudio.mcpchat.ui.theme.Dimens
import com.aistudio.mcpchat.ui.theme.Spacing
import com.aistudio.mcpchat.ui.viewmodel.ThemeViewModel

@Composable
fun SettingsScreen(viewModel: ThemeViewModel, modifier: Modifier = Modifier) {
    val prefs by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreenContent(
        prefs = prefs,
        onSelectMode = viewModel::setThemeMode,
        onAmoledChange = viewModel::setUseAmoledDark,
        onDynamicChange = viewModel::setUseDynamicColor,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    prefs: com.aistudio.mcpchat.data.local.ThemePreferences,
    onSelectMode: (ThemeMode) -> Unit,
    onAmoledChange: (Boolean) -> Unit,
    onDynamicChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Column {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Customize chat theme & colors",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.iconSizeLg),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Theme mode",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ThemeModeChip(
                            label = "System",
                            icon = Icons.Default.BrightnessAuto,
                            selected = prefs.themeMode == ThemeMode.SYSTEM,
                            onClick = { onSelectMode(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeModeChip(
                            label = "Light",
                            icon = Icons.Default.LightMode,
                            selected = prefs.themeMode == ThemeMode.LIGHT,
                            onClick = { onSelectMode(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeModeChip(
                            label = "Dark",
                            icon = Icons.Default.DarkMode,
                            selected = prefs.themeMode == ThemeMode.DARK,
                            onClick = { onSelectMode(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = when (prefs.themeMode) {
                            ThemeMode.SYSTEM -> "Follows system dark / light setting"
                            ThemeMode.LIGHT -> "Always light"
                            ThemeMode.DARK -> "Always dark (AMOLED optional)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                    SettingSwitchRow(
                        title = "AMOLED black",
                        subtitle = "Pure black background in dark mode. Saves battery on OLED.",
                        checked = prefs.useAmoledDark,
                        enabled = prefs.themeMode != ThemeMode.LIGHT,
                        onCheckedChange = onAmoledChange,
                        testTag = "amoled_switch",
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    SettingSwitchRow(
                        title = "Dynamic color (Android 12+)",
                        subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "Use system Material You colors when enabled"
                        } else {
                            "Requires Android 12 or higher"
                        },
                        checked = prefs.useDynamicColor,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        onCheckedChange = onDynamicChange,
                        testTag = "dynamic_color_switch",
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(Dimens.cornerRadiusLg),
                border = BorderStroke(Dimens.borderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        PreviewBubble(text = "Hello with MCP tools!", isUser = true, modifier = Modifier.weight(1f))
                        PreviewBubble(text = "Hi! How can I help?", isUser = false, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    CodeBlock(
                        code = """{"tool": "weather", "args": {"location": "Tokyo"}}""",
                        language = "json",
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconSizeSm))
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier
            .testTag("theme_chip_${label.lowercase()}")
            .semantics {
                role = Role.Button
                contentDescription = "Select $label theme"
                stateDescription = if (selected) "Selected" else "Not selected"
            },
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = title
                stateDescription = if (checked) "On" else "Off"
            },
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .testTag(testTag)
                .semantics {
                    contentDescription = title
                    stateDescription = if (checked) "Enabled" else "Disabled"
                },
        )
    }
}

@Composable
private fun PreviewBubble(text: String, isUser: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(Dimens.cornerRadiusLg),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        )
    }
}
