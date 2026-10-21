package com.yc.navigator.ui.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.yc.navigator.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AircraftTypeField(
    value: String,
    types: List<String>,
    onValueChange: (String) -> Unit,
    onSaveType: (String) -> Unit,
    onRemoveType: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var removedCurrentValue by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val removeDescription = stringResource(R.string.aircraft_type_remove)
    val showMenu = expanded && types.isNotEmpty()

    fun saveCurrentType() {
        if (!value.equals(removedCurrentValue, ignoreCase = true)) {
            onSaveType(value)
        }
        removedCurrentValue = null
    }

    ExposedDropdownMenuBox(
        expanded = showMenu,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                removedCurrentValue = null
                onValueChange(it)
                expanded = true
            },
            label = { Text(stringResource(R.string.field_aircraft_type)) },
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenu)
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                saveCurrentType()
                expanded = false
                focusManager.clearFocus()
            }),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (focused && !state.isFocused) {
                        saveCurrentType()
                        expanded = false
                    }
                    focused = state.isFocused
                }
        )

        ExposedDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { expanded = false }
        ) {
            types.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        removedCurrentValue = null
                        onValueChange(type)
                        onSaveType(type)
                        expanded = false
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            onRemoveType(type)
                            if (value.equals(type, ignoreCase = true)) {
                                removedCurrentValue = type
                            }
                        }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = removeDescription
                            )
                        }
                    }
                )
            }
        }
    }
}
