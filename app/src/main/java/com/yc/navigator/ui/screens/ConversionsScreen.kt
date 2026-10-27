package com.yc.navigator.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yc.navigator.R
import com.yc.navigator.domain.FuelUnit
import com.yc.navigator.domain.convertFuel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionsScreen(
    languageTag: String,
    onBack: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    var selectedUnitIndex by rememberSaveable { mutableIntStateOf(0) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var speedInput by rememberSaveable { mutableStateOf("") }
    var speedUnitIndex by rememberSaveable { mutableIntStateOf(0) }
    var speedUnitMenuExpanded by remember { mutableStateOf(false) }
    val selectedUnit = FuelUnit.entries[selectedUnitIndex]
    val speedUnit = SpeedUnit.entries[speedUnitIndex]
    val numberFormat = remember(languageTag) {
        NumberFormat.getNumberInstance(
            Locale.forLanguageTag(if (languageTag == "en") "en" else "ru-RU")
        ).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
    }
    val parsedValue = input.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() }
    val parsedSpeed = speedInput.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "YC NAVIGATOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.conversions_title),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.conversions_density_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.conversions_value)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                ExposedDropdownMenuBox(
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = it },
                    modifier = Modifier.width(170.dp)
                ) {
                    OutlinedTextField(
                        value = fuelUnitLabel(selectedUnit),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.conversions_unit)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = unitMenuExpanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        FuelUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(fuelUnitLabel(unit)) },
                                onClick = {
                                    selectedUnitIndex = unit.ordinal
                                    unitMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.conversions_result),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            when {
                input.isBlank() -> ConversionHint(
                    text = stringResource(R.string.conversions_enter_value)
                )

                parsedValue == null -> ConversionHint(
                    text = stringResource(R.string.conversions_invalid_value)
                )

                else -> {
                    val targets = FuelUnit.entries.filter { it != selectedUnit }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            targets.forEachIndexed { index, unit ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = numberFormat.format(
                                            convertFuel(parsedValue, selectedUnit, unit)
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = fuelUnitLabel(unit),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.conversions_source_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.quick_conversions),
                contentDescription = stringResource(R.string.conversions_source_note),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1198f / 1076f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    )
            )
            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.conversions_speed),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.conversions_speed_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = speedInput,
                    onValueChange = { speedInput = it },
                    label = { Text(stringResource(R.string.conversions_value)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                ExposedDropdownMenuBox(
                    expanded = speedUnitMenuExpanded,
                    onExpandedChange = { speedUnitMenuExpanded = it },
                    modifier = Modifier.width(170.dp)
                ) {
                    OutlinedTextField(
                        value = speedUnitLabel(speedUnit),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.conversions_unit)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = speedUnitMenuExpanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = speedUnitMenuExpanded,
                        onDismissRequest = { speedUnitMenuExpanded = false }
                    ) {
                        SpeedUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(speedUnitLabel(unit)) },
                                onClick = {
                                    speedUnitIndex = unit.ordinal
                                    speedUnitMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.conversions_result),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            when {
                speedInput.isBlank() -> ConversionHint(
                    text = stringResource(R.string.conversions_enter_value)
                )

                parsedSpeed == null -> ConversionHint(
                    text = stringResource(R.string.conversions_invalid_value)
                )

                else -> {
                    val targetUnit = if (speedUnit == SpeedUnit.KMH) {
                        SpeedUnit.KNOTS
                    } else {
                        SpeedUnit.KMH
                    }
                    val result = parsedSpeed * speedUnit.kmhPerUnit / targetUnit.kmhPerUnit
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = numberFormat.format(result),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = speedUnitLabel(targetUnit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}

private enum class SpeedUnit(val kmhPerUnit: Double) {
    KMH(1.0),
    KNOTS(1.852)
}

@Composable
private fun ConversionHint(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Composable
private fun fuelUnitLabel(unit: FuelUnit): String = stringResource(
    when (unit) {
        FuelUnit.LITERS -> R.string.conversions_unit_liters
        FuelUnit.US_GALLONS -> R.string.conversions_unit_us_gallons
        FuelUnit.IMPERIAL_GALLONS -> R.string.conversions_unit_imperial_gallons
        FuelUnit.KILOGRAMS -> R.string.conversions_unit_kilograms
        FuelUnit.POUNDS -> R.string.conversions_unit_pounds
    }
)

@Composable
private fun speedUnitLabel(unit: SpeedUnit): String = stringResource(
    when (unit) {
        SpeedUnit.KMH -> R.string.conversions_unit_kmh
        SpeedUnit.KNOTS -> R.string.conversions_unit_knots
    }
)
