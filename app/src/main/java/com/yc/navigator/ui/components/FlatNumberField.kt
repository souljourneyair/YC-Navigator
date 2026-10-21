package com.yc.navigator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yc.navigator.data.Quantity
import com.yc.navigator.data.UnitConverter
import com.yc.navigator.data.UnitSystem

@Composable
fun FlatNumberField(
    canonicalValue: Double?,
    onCanonicalChange: (Double?) -> Unit,
    quantity: Quantity,
    units: UnitSystem,
    modifier: Modifier = Modifier,
    maxDecimals: Int = 1,
    unitText: String? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    val unitLabel = unitText ?: quantityUnitLabel(quantity, units)
    var focused by remember { mutableStateOf(false) }
    var text by rememberSaveable(units.name) {
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

    val shape = RoundedCornerShape(6.dp)
    val fieldBackground = if (focused) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val fieldBorder = if (focused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = { raw ->
                text = raw
                onCanonicalChange(UnitConverter.parse(raw, quantity, units))
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .background(fieldBackground)
                .border(if (focused) 2.dp else 1.dp, fieldBorder, shape)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .onFocusChanged { state ->
                    focused = state.isFocused
                    if (!state.isFocused) {
                        text = canonicalValue?.let {
                            UnitConverter.format(it, quantity, units, locale, maxDecimals)
                        } ?: ""
                    }
                },
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (!unitLabel.isNullOrBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
