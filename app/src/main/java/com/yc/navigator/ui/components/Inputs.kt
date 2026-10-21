package com.yc.navigator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yc.navigator.R
import com.yc.navigator.data.Quantity
import com.yc.navigator.data.UnitConverter
import com.yc.navigator.data.UnitSystem
import com.yc.navigator.ui.appLocale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    collapsible: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable(title, collapsible) { mutableStateOf(!collapsible) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (collapsible) {
                            Modifier.clickable { expanded = !expanded }
                        } else {
                            Modifier
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (trailing != null) trailing()
                if (collapsible) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Filled.KeyboardArrowUp
                        } else {
                            Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!collapsible || expanded) {
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
fun HelpDot(
    title: String,
    help: String,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(start = 4.dp)
            .size(18.dp)
            .clip(CircleShape)
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                CircleShape
            )
            .clickable { open = true },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = { Text(help) },
            confirmButton = {
                TextButton(onClick = { open = false }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
fun FieldLabel(
    text: String,
    help: String?
) {
    if (help == null) {
        Text(text)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text)
            HelpDot(title = text, help = help)
        }
    }
}

@Composable
fun TextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    placeholder: String? = null,
    help: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { FieldLabel(label, help) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun NumberField(
    label: String,
    canonicalValue: Double?,
    onCanonicalChange: (Double?) -> Unit,
    quantity: Quantity,
    units: UnitSystem,
    modifier: Modifier = Modifier,
    maxDecimals: Int = 1,
    unitText: String? = null,
    enabled: Boolean = true,
    help: String? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    val unitLabel = unitText ?: quantityUnitLabel(quantity, units)
    var focused by remember { mutableStateOf(false) }
    var text by rememberSaveable(units.name, label) {
        mutableStateOf(
            canonicalValue?.let {
                UnitConverter.format(it, quantity, units, locale, maxDecimals)
            } ?: ""
        )
    }

    LaunchedEffect(canonicalValue, units, locale) {
        if (!focused) {
            text = canonicalValue?.let {
                UnitConverter.format(it, quantity, units, locale, maxDecimals)
            } ?: ""
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            onCanonicalChange(UnitConverter.parse(raw, quantity, units))
        },
        label = { FieldLabel(label, help) },
        suffix = unitLabel?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                focused = state.isFocused
                if (!state.isFocused) {
                    text = canonicalValue?.let {
                        UnitConverter.format(it, quantity, units, locale, maxDecimals)
                    } ?: ""
                }
            }
    )
}

@Composable
fun IntField(
    label: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    enabled: Boolean = true,
    help: String? = null
) {
    var focused by remember { mutableStateOf(false) }
    var text by rememberSaveable(label) { mutableStateOf(value?.toString() ?: "") }

    LaunchedEffect(value) {
        if (!focused) text = value?.toString() ?: ""
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val filtered = raw.filter(Char::isDigit)
            text = filtered
            onValueChange(filtered.toIntOrNull())
        },
        label = { FieldLabel(label, help) },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
    )
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    isoDate: String,
    onDateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val display = runCatching {
        LocalDate.parse(isoDate).format(
            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy", locale)
        )
    }.getOrDefault(isoDate)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = display.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPicker) {
        val initialMillis = runCatching {
            LocalDate.parse(isoDate)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDateChange(date.toString())
                    }
                    showPicker = false
                }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.action_choose))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun <T> SelectionDialog(
    title: String,
    items: List<T>,
    labelFor: (T) -> String,
    onSelect: (T?) -> Unit,
    onDismiss: () -> Unit,
    noneLabel: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    Text(
                        text = noneLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(null)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(items) { item ->
                    Text(
                        text = labelFor(item),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(item)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun MoveButtons(
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.IconButton(
            onClick = { onMoveUp?.invoke() },
            enabled = onMoveUp != null,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = androidx.compose.ui.res.stringResource(R.string.action_move_up)
            )
        }
        androidx.compose.material3.IconButton(
            onClick = { onMoveDown?.invoke() },
            enabled = onMoveDown != null,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = androidx.compose.ui.res.stringResource(R.string.action_move_down)
            )
        }
    }
}

@Composable
fun HorizontalGap(value: Int = 8) {
    Spacer(Modifier.width(value.dp))
}

@Composable
fun VerticalGap(value: Int = 8) {
    Spacer(Modifier.height(value.dp))
}

@Composable
fun Boxed() {
    Box(Modifier)
}
