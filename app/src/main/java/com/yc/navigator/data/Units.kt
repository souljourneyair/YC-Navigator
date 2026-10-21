package com.yc.navigator.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class UnitSystem {
    RU,
    INTERNATIONAL
}

enum class Quantity {
    NONE,
    ALTITUDE,
    SPEED,
    DISTANCE,
    MASS,
    PRESSURE,
    TEMPERATURE,
    FUEL_RATE
}

fun AppSettings.effectiveUnitSystem(): UnitSystem = when (unitMode) {
    UnitMode.AUTO -> if (languageTag == "en") UnitSystem.INTERNATIONAL else UnitSystem.RU
    UnitMode.RU -> UnitSystem.RU
    UnitMode.INTERNATIONAL -> UnitSystem.INTERNATIONAL
}

object UnitConverter {

    private const val METERS_TO_FEET = 3.280839895
    private const val KMH_TO_KNOTS = 1.0 / 1.852
    private const val KM_TO_NM = 1.0 / 1.852
    private const val KG_TO_LB = 2.2046226218
    private const val MMHG_TO_INHG = 1.0 / 25.4

    fun toDisplay(value: Double, quantity: Quantity, system: UnitSystem): Double {
        if (system == UnitSystem.RU) return value
        return when (quantity) {
            Quantity.ALTITUDE -> value * METERS_TO_FEET
            Quantity.SPEED -> value * KMH_TO_KNOTS
            Quantity.DISTANCE -> value * KM_TO_NM
            Quantity.MASS, Quantity.FUEL_RATE -> value * KG_TO_LB
            Quantity.PRESSURE -> value * MMHG_TO_INHG
            Quantity.TEMPERATURE -> value * 9.0 / 5.0 + 32.0
            Quantity.NONE -> value
        }
    }

    fun toCanonical(value: Double, quantity: Quantity, system: UnitSystem): Double {
        if (system == UnitSystem.RU) return value
        return when (quantity) {
            Quantity.ALTITUDE -> value / METERS_TO_FEET
            Quantity.SPEED -> value / KMH_TO_KNOTS
            Quantity.DISTANCE -> value / KM_TO_NM
            Quantity.MASS, Quantity.FUEL_RATE -> value / KG_TO_LB
            Quantity.PRESSURE -> value / MMHG_TO_INHG
            Quantity.TEMPERATURE -> (value - 32.0) * 5.0 / 9.0
            Quantity.NONE -> value
        }
    }

    fun format(
        canonicalValue: Double?,
        quantity: Quantity,
        system: UnitSystem,
        locale: Locale,
        maxDecimals: Int = 1
    ): String {
        if (canonicalValue == null || canonicalValue.isNaN() || canonicalValue.isInfinite()) {
            return ""
        }
        val display = toDisplay(canonicalValue, quantity, system)
        return formatPlain(display, locale, maxDecimals)
    }

    fun formatPlain(value: Double, locale: Locale, maxDecimals: Int = 1): String {
        if (value.isNaN() || value.isInfinite()) return ""
        val decimals = maxDecimals.coerceIn(0, 4)
        val pattern = if (decimals == 0) "0" else "0." + "#".repeat(decimals)
        val symbols = DecimalFormatSymbols(locale)
        val formatter = DecimalFormat(pattern, symbols)
        formatter.isGroupingUsed = false
        return formatter.format(if (value == -0.0) 0.0 else value)
    }

    fun parse(
        text: String,
        quantity: Quantity,
        system: UnitSystem
    ): Double? {
        val normalized = text
            .replace('\u00A0', ' ')
            .replace(" ", "")
            .replace(',', '.')
            .trim()
        if (normalized.isEmpty()) return null
        val parsed = normalized.toDoubleOrNull() ?: return null
        return toCanonical(parsed, quantity, system)
    }
}
