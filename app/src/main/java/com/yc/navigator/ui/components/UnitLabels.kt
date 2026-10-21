package com.yc.navigator.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yc.navigator.R
import com.yc.navigator.data.Quantity
import com.yc.navigator.data.UnitSystem

@Composable
fun quantityUnitLabel(quantity: Quantity, units: UnitSystem): String? = when (quantity) {
    Quantity.NONE -> null
    Quantity.ALTITUDE ->
        stringResource(if (units == UnitSystem.RU) R.string.unit_m else R.string.unit_ft)
    Quantity.SPEED ->
        stringResource(if (units == UnitSystem.RU) R.string.unit_kmh else R.string.unit_kt)
    Quantity.DISTANCE ->
        stringResource(if (units == UnitSystem.RU) R.string.unit_km else R.string.unit_nm)
    Quantity.MASS ->
        stringResource(if (units == UnitSystem.RU) R.string.unit_kg else R.string.unit_lb)
    Quantity.PRESSURE ->
        stringResource(if (units == UnitSystem.RU) R.string.unit_mmhg else R.string.unit_inhg)
    Quantity.TEMPERATURE ->
        stringResource(if (units == UnitSystem.RU) R.string.unit_c else R.string.unit_f)
    Quantity.FUEL_RATE ->
        stringResource(if (units == UnitSystem.RU) R.string.unit_kg_h else R.string.unit_lb_h)
}
