package com.yc.navigator.domain

enum class FuelUnit {
    LITERS,
    US_GALLONS,
    IMPERIAL_GALLONS,
    KILOGRAMS,
    POUNDS
}

fun convertFuel(value: Double, from: FuelUnit, to: FuelUnit): Double {
    if (from == to) return value
    return when (from) {
        FuelUnit.LITERS -> when (to) {
            FuelUnit.US_GALLONS -> value / 3.8
            FuelUnit.IMPERIAL_GALLONS -> value / 4.5
            FuelUnit.KILOGRAMS -> value * 0.72
            FuelUnit.POUNDS -> value * 1.58
            FuelUnit.LITERS -> value
        }

        FuelUnit.US_GALLONS -> when (to) {
            FuelUnit.LITERS -> value * 3.8
            FuelUnit.IMPERIAL_GALLONS -> value / 1.2
            FuelUnit.KILOGRAMS -> value * 2.72
            FuelUnit.POUNDS -> value * 6.0
            FuelUnit.US_GALLONS -> value
        }

        FuelUnit.IMPERIAL_GALLONS -> when (to) {
            FuelUnit.LITERS -> value * 4.5
            FuelUnit.US_GALLONS -> value * 1.2
            FuelUnit.KILOGRAMS -> value * 3.27
            FuelUnit.POUNDS -> value * 7.2
            FuelUnit.IMPERIAL_GALLONS -> value
        }

        FuelUnit.KILOGRAMS -> when (to) {
            FuelUnit.LITERS -> value / 0.72
            FuelUnit.US_GALLONS -> value / 2.72
            FuelUnit.IMPERIAL_GALLONS -> value / 3.27
            FuelUnit.POUNDS -> value * 2.2
            FuelUnit.KILOGRAMS -> value
        }

        FuelUnit.POUNDS -> when (to) {
            FuelUnit.LITERS -> value / 1.58
            FuelUnit.US_GALLONS -> value / 6.0
            FuelUnit.IMPERIAL_GALLONS -> value / 7.2
            FuelUnit.KILOGRAMS -> value / 2.2
            FuelUnit.POUNDS -> value
        }
    }
}
