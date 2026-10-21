package com.yc.navigator.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConversionTest {

    @Test
    fun `international units convert and back`() {
        assertEquals(123.65, UnitConverter.toDisplay(229.0, Quantity.DISTANCE, UnitSystem.INTERNATIONAL), 0.01)
        assertEquals(229.0, UnitConverter.toCanonical(123.65, Quantity.DISTANCE, UnitSystem.INTERNATIONAL), 0.01)

        assertEquals(168.65, UnitConverter.toDisplay(76.5, Quantity.MASS, UnitSystem.INTERNATIONAL), 0.01)
        assertEquals(76.5, UnitConverter.toCanonical(168.65, Quantity.MASS, UnitSystem.INTERNATIONAL), 0.01)

        assertEquals(1640.42, UnitConverter.toDisplay(500.0, Quantity.ALTITUDE, UnitSystem.INTERNATIONAL), 0.01)
        assertEquals(500.0, UnitConverter.toCanonical(1640.42, Quantity.ALTITUDE, UnitSystem.INTERNATIONAL), 0.01)

        assertEquals(29.33, UnitConverter.toDisplay(745.0, Quantity.PRESSURE, UnitSystem.INTERNATIONAL), 0.01)
        assertEquals(745.0, UnitConverter.toCanonical(29.33, Quantity.PRESSURE, UnitSystem.INTERNATIONAL), 0.1)

        assertEquals(12.2, UnitConverter.toDisplay(-11.0, Quantity.TEMPERATURE, UnitSystem.INTERNATIONAL), 0.01)
        assertEquals(-11.0, UnitConverter.toCanonical(12.2, Quantity.TEMPERATURE, UnitSystem.INTERNATIONAL), 0.01)
    }

    @Test
    fun `russian units stay unchanged`() {
        assertEquals(229.0, UnitConverter.toDisplay(229.0, Quantity.DISTANCE, UnitSystem.RU), 0.0)
        assertEquals(-11.0, UnitConverter.toDisplay(-11.0, Quantity.TEMPERATURE, UnitSystem.RU), 0.0)
    }

    @Test
    fun `effective unit system follows language unless overridden`() {
        assertEquals(UnitSystem.RU, AppSettings("ru", UnitMode.AUTO).effectiveUnitSystem())
        assertEquals(UnitSystem.INTERNATIONAL, AppSettings("en", UnitMode.AUTO).effectiveUnitSystem())
        assertEquals(UnitSystem.RU, AppSettings("en", UnitMode.RU).effectiveUnitSystem())
        assertEquals(
            UnitSystem.INTERNATIONAL,
            AppSettings("ru", UnitMode.INTERNATIONAL).effectiveUnitSystem()
        )
    }
}

