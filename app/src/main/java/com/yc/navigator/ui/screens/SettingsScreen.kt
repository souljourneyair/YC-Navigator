package com.yc.navigator.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yc.navigator.BuildConfig
import com.yc.navigator.R
import com.yc.navigator.data.AppSettings
import com.yc.navigator.data.ThemeMode
import com.yc.navigator.data.UnitMode
import com.yc.navigator.ui.components.SectionCard

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onLanguageChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onUnitModeChange: (UnitMode) -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit
) {
    var showImportConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "YC NAVIGATOR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(18.dp))

        SectionCard(title = stringResource(R.string.settings_language)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.languageTag == "ru",
                    onClick = { onLanguageChange("ru") },
                    label = { Text(stringResource(R.string.settings_russian)) }
                )
                FilterChip(
                    selected = settings.languageTag == "en",
                    onClick = { onLanguageChange("en") },
                    label = { Text(stringResource(R.string.settings_english)) }
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.settings_language_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        SectionCard(title = stringResource(R.string.settings_theme)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    label = { Text(stringResource(R.string.settings_theme_system)) }
                )
                FilterChip(
                    selected = settings.themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    label = { Text(stringResource(R.string.settings_theme_light)) }
                )
                FilterChip(
                    selected = settings.themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    label = { Text(stringResource(R.string.settings_theme_dark)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SectionCard(title = stringResource(R.string.settings_units)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.unitMode == UnitMode.AUTO,
                    onClick = { onUnitModeChange(UnitMode.AUTO) },
                    label = { Text(stringResource(R.string.settings_units_auto)) }
                )
                FilterChip(
                    selected = settings.unitMode == UnitMode.RU,
                    onClick = { onUnitModeChange(UnitMode.RU) },
                    label = { Text(stringResource(R.string.settings_units_ru)) }
                )
                FilterChip(
                    selected = settings.unitMode == UnitMode.INTERNATIONAL,
                    onClick = { onUnitModeChange(UnitMode.INTERNATIONAL) },
                    label = { Text(stringResource(R.string.settings_units_intl)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SectionCard(title = stringResource(R.string.settings_data)) {
            Text(
                text = stringResource(R.string.settings_data_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onExportData) {
                    Text(stringResource(R.string.action_export))
                }
                TextButton(onClick = { showImportConfirm = true }) {
                    Text(stringResource(R.string.action_import))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SectionCard(title = stringResource(R.string.settings_about)) {
            Image(
                painter = painterResource(R.drawable.logo_small),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.settings_app_description),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_author),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_vibe_coding),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_local_only),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(96.dp))
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text(stringResource(R.string.settings_import_confirm_title)) },
            text = { Text(stringResource(R.string.settings_import_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    onImportData()
                }) {
                    Text(stringResource(R.string.action_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
