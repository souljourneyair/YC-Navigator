package com.yc.navigator.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yc.navigator.R
import com.yc.navigator.data.Pilot
import com.yc.navigator.data.Quantity
import com.yc.navigator.data.UnitConverter
import com.yc.navigator.data.UnitSystem
import com.yc.navigator.ui.components.HelpDot
import com.yc.navigator.ui.components.PickerField
import com.yc.navigator.ui.components.SelectionDialog
import com.yc.navigator.ui.components.quantityUnitLabel

@Composable
fun EditorItem(
    content: @Composable (Modifier) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(Modifier.widthIn(max = 960.dp).fillMaxWidth()) {
            content(Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ValueField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    help: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (help != null) {
                    HelpDot(title = label, help = help)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (highlight) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun formattedQuantity(
    value: Double?,
    quantity: Quantity,
    units: UnitSystem,
    maxDecimals: Int = 1
): String {
    if (value == null) return "—"
    val locale = LocalConfiguration.current.locales[0]
    val number = UnitConverter.format(value, quantity, units, locale, maxDecimals)
    if (number.isBlank()) return "—"
    val unit = quantityUnitLabel(quantity, units)
    return if (unit.isNullOrBlank()) number else "$number $unit"
}

@Composable
fun formattedNumber(value: Double?, maxDecimals: Int = 1): String {
    if (value == null) return "—"
    val locale = LocalConfiguration.current.locales[0]
    return UnitConverter.formatPlain(value, locale, maxDecimals)
}

@Composable
fun PilotPickerField(
    roleLabel: String,
    selectedPilotId: Long?,
    pilots: List<Pilot>,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val unassigned = stringResource(R.string.role_unassigned)
    val selectedName = pilots.firstOrNull { it.id == selectedPilotId }?.name ?: unassigned

    Column(modifier) {
        PickerField(
            label = roleLabel,
            value = selectedName,
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        )
        if (pilots.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.msg_no_pilots),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDialog) {
        SelectionDialog(
            title = stringResource(R.string.field_choose_pilot),
            items = pilots,
            labelFor = { it.name },
            onSelect = { pilot -> onSelect(pilot?.id) },
            onDismiss = { showDialog = false },
            noneLabel = stringResource(R.string.field_no_pilot)
        )
    }
}
