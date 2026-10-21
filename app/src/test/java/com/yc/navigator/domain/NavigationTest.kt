package com.yc.navigator.domain

import com.yc.navigator.data.FuelParams
import com.yc.navigator.data.Journal
import com.yc.navigator.data.Leg
import com.yc.navigator.data.WindWeather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NavigationTest {

    private val epsilon = 0.001

    @Test
    fun `wind direction and angles match the Excel example`() {
        val deltaMagnetic = Navigation.windDirectionMagnetic(330.0, 10.0)
        assertEquals(140.0, deltaMagnetic!!, epsilon)

        val windAngle = Navigation.windAngle(deltaMagnetic, 224.0)
        assertEquals(276.0, windAngle!!, epsilon)

        val drift = Navigation.driftAngle(30.0, windAngle, 180.0)
        assertEquals(-9.5410182728, drift!!, epsilon)

        val course = Navigation.magneticCourse(224.0, drift)
        assertEquals(233.5410182728, course!!, epsilon)

        val groundSpeed = Navigation.groundSpeed(180.0, 30.0, windAngle, drift)
        assertEquals(181.0, groundSpeed!!, epsilon)

        assertEquals(5, Navigation.legTimeMinutes(15.0, groundSpeed))
    }

    @Test
    fun `safe altitude matches the Excel example`() {
        assertEquals(395.0, Navigation.safeAltitude(263.0, -11.0)!!, epsilon)
    }

    @Test
    fun `safe levels match the Excel example`() {
        val levels = FlightLevels.safeLevels(
            obstacleHeightM = 263.0,
            temperatureC = -11.0,
            minPressureMmHg = 763.0,
            trueTrackDeg = 234.0
        )
        assertEquals(50, levels.ifr)
        assertEquals(40, levels.vfr)
    }

    @Test
    fun `fuel is calculated backwards from the reserve`() {
        val times = listOf(5, 6, 5, 17, 7, 14, 16, 3, 10)
        val legs = times.mapIndexed { index, minutes ->
            Leg(
                id = index.toLong(),
                route = "P$index",
                magneticTrack = 0.0,
                trueAirspeedKmh = 180.0,
                distanceKm = minutes / 60.0 * 180.0
            )
        }

        val journal = Journal(
            id = 1,
            legs = legs,
            takeoffTimeMinutes = 4,
            landingTimeMinutes = 6,
            windWeather = WindWeather(
                windDirectionTrue = 0.0,
                magneticDeclination = 0.0,
                windSpeedKmh = 0.0
            ),
            fuel = FuelParams(
                reserveKg = 30.0,
                hourlyConsumptionKg = 30.0
            ),
            departure = com.yc.navigator.data.Aerodrome(name = "Test")
        )

        val result = JournalCalculator.calculate(journal)

        assertEquals(93, result.totalTimeMinutes)
        assertEquals(76.5, result.totalFuelKg!!, epsilon)
        assertEquals(74.5, result.legs.first().fuelRemainingKg!!, epsilon)
        assertEquals(33.0, result.landingFuelKg!!, epsilon)
        assertEquals(76.5, result.takeoffFuelKg!!, epsilon)
        assertNotNull(result.legs.first().magneticCourseDeg)
    }
}
