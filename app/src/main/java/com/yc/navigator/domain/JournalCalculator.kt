package com.yc.navigator.domain

import com.yc.navigator.data.Journal
import com.yc.navigator.data.Leg

data class LegCalculation(
    val magneticCourseDeg: Double? = null,
    val groundSpeedKmh: Double? = null,
    val windAngleDeg: Double? = null,
    val driftAngleDeg: Double? = null,
    val timeMinutes: Int? = null,
    val safeAltitudeM: Double? = null,
    val fuelRemainingKg: Double? = null,
    val windExceedsAirspeed: Boolean = false
)

data class JournalCalculation(
    val legs: List<LegCalculation>,
    val landingFuelKg: Double?,
    val takeoffFuelKg: Double?,
    val totalDistanceKm: Double,
    val totalTimeMinutes: Int?,
    val totalFuelKg: Double?,
    val windDirectionMagneticDeg: Double?,
    val safeLevels: SafeLevels
)

object JournalCalculator {

    fun calculate(journal: Journal): JournalCalculation {
        val windDirectionMagnetic = Navigation.windDirectionMagnetic(
            journal.windWeather.windDirectionTrue,
            journal.windWeather.magneticDeclination
        )
        val windSpeed = journal.windWeather.windSpeedKmh
        val temperature = journal.departure.temperatureC

        val legCalculations = journal.legs.map { leg ->
            calculateLeg(
                leg = leg,
                windDirectionMagnetic = windDirectionMagnetic,
                windSpeedKmh = windSpeed,
                temperatureC = temperature
            )
        }

        val totalDistance = journal.legs.mapNotNull { it.distanceKm }.sum()
        val totalTime = totalMinutes(
            takeoffMinutes = journal.takeoffTimeMinutes,
            legMinutes = legCalculations.map { it.timeMinutes },
            landingMinutes = journal.landingTimeMinutes
        )

        val safeLevels = calculateSafeLevels(journal)

        return applyFuel(
            journal = journal,
            legCalcs = legCalculations,
            totalDistance = totalDistance,
            totalTime = totalTime
        ).copy(
            windDirectionMagneticDeg = windDirectionMagnetic,
            safeLevels = safeLevels
        )
    }

    fun windErrorDetected(calculation: JournalCalculation): Boolean =
        calculation.legs.any { it.windExceedsAirspeed }

    private fun calculateLeg(
        leg: Leg,
        windDirectionMagnetic: Double?,
        windSpeedKmh: Double?,
        temperatureC: Double?
    ): LegCalculation {
        val windAngle = Navigation.windAngle(windDirectionMagnetic, leg.magneticTrack)
        val drift = Navigation.driftAngle(windSpeedKmh, windAngle, leg.trueAirspeedKmh)
        val course = Navigation.magneticCourse(leg.magneticTrack, drift)
        val groundSpeed = Navigation.groundSpeed(
            trueAirspeedKmh = leg.trueAirspeedKmh,
            windSpeedKmh = windSpeedKmh,
            windAngleDeg = windAngle,
            driftAngleDeg = drift
        )
        val time = Navigation.legTimeMinutes(leg.distanceKm, groundSpeed)
        val safeAltitude = Navigation.safeAltitude(leg.obstacleHeightM, temperatureC)

        val windError = windSpeedKmh != null &&
            windAngle != null &&
            leg.trueAirspeedKmh != null &&
            leg.trueAirspeedKmh > 0.0 &&
            (drift == null || groundSpeed == null || groundSpeed <= 0.0)

        return LegCalculation(
            magneticCourseDeg = course,
            groundSpeedKmh = groundSpeed,
            windAngleDeg = windAngle,
            driftAngleDeg = drift,
            timeMinutes = time,
            safeAltitudeM = safeAltitude,
            windExceedsAirspeed = windError
        )
    }

    private fun calculateSafeLevels(journal: Journal): SafeLevels {
        val firstLeg = journal.legs.firstOrNull()
        val trueTrack = firstLeg?.magneticTrack?.let { track ->
            Navigation.normalize360(
                track + (journal.windWeather.magneticDeclination ?: 0.0)
            )
        }
        return FlightLevels.safeLevels(
            obstacleHeightM = firstLeg?.obstacleHeightM,
            temperatureC = journal.departure.temperatureC,
            minPressureMmHg = journal.windWeather.minPressureMmHg,
            trueTrackDeg = trueTrack
        )
    }

    private fun applyFuel(
        journal: Journal,
        legCalcs: List<LegCalculation>,
        totalDistance: Double,
        totalTime: Int?
    ): JournalCalculation {
        val hourly = journal.fuel.hourlyConsumptionKg
        val reserve = journal.fuel.reserveKg

        if (hourly == null || reserve == null) {
            return JournalCalculation(
                legs = legCalcs,
                landingFuelKg = null,
                takeoffFuelKg = null,
                totalDistanceKm = totalDistance,
                totalTimeMinutes = totalTime,
                totalFuelKg = null,
                windDirectionMagneticDeg = null,
                safeLevels = SafeLevels(null, null)
            )
        }

        var current = reserve + (journal.landingTimeMinutes ?: 0) / 60.0 * hourly
        val landingFuel = current
        val fuelByLeg = arrayOfNulls<Double>(legCalcs.size)

        for (index in legCalcs.indices.reversed()) {
            val minutes = legCalcs[index].timeMinutes ?: 0
            current += minutes / 60.0 * hourly
            fuelByLeg[index] = current
        }

        val takeoffFuel = current + (journal.takeoffTimeMinutes ?: 0) / 60.0 * hourly

        return JournalCalculation(
            legs = legCalcs.mapIndexed { index, calc ->
                calc.copy(fuelRemainingKg = fuelByLeg[index])
            },
            landingFuelKg = landingFuel,
            takeoffFuelKg = takeoffFuel,
            totalDistanceKm = totalDistance,
            totalTimeMinutes = totalTime,
            totalFuelKg = takeoffFuel,
            windDirectionMagneticDeg = null,
            safeLevels = SafeLevels(null, null)
        )
    }

    private fun totalMinutes(
        takeoffMinutes: Int?,
        legMinutes: List<Int?>,
        landingMinutes: Int?
    ): Int? {
        val all = buildList {
            takeoffMinutes?.let(::add)
            addAll(legMinutes.filterNotNull())
            landingMinutes?.let(::add)
        }
        return if (all.isEmpty()) null else all.sum()
    }
}
