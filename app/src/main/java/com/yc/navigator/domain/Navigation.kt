package com.yc.navigator.domain

import kotlin.math.ceil
import kotlin.math.round

object Navigation {

    fun normalize360(value: Double): Double {
        var result = value % 360.0
        if (result < 0.0) result += 360.0
        return result
    }

    fun windDirectionMagnetic(windDirectionTrue: Double?, declination: Double?): Double? {
        if (windDirectionTrue == null || declination == null) return null
        return normalize360(windDirectionTrue - declination + 180.0)
    }

    fun windAngle(windDirectionMagnetic: Double?, magneticTrack: Double?): Double? {
        if (windDirectionMagnetic == null || magneticTrack == null) return null
        return normalize360(windDirectionMagnetic - magneticTrack)
    }

    fun driftAngle(
        windSpeedKmh: Double?,
        windAngleDeg: Double?,
        trueAirspeedKmh: Double?
    ): Double? {
        if (windSpeedKmh == null || windAngleDeg == null || trueAirspeedKmh == null) {
            return null
        }
        if (trueAirspeedKmh <= 0.0) return null
        val ratio = windSpeedKmh * kotlin.math.sin(Math.toRadians(windAngleDeg)) / trueAirspeedKmh
        if (ratio.isNaN() || ratio.isInfinite() || kotlin.math.abs(ratio) > 1.0) return null
        return Math.toDegrees(kotlin.math.asin(ratio))
    }

    fun magneticCourse(magneticTrack: Double?, driftAngleDeg: Double?): Double? {
        if (magneticTrack == null || driftAngleDeg == null) return null
        return normalize360(magneticTrack - driftAngleDeg)
    }

    fun groundSpeed(
        trueAirspeedKmh: Double?,
        windSpeedKmh: Double?,
        windAngleDeg: Double?,
        driftAngleDeg: Double?
    ): Double? {
        if (
            trueAirspeedKmh == null ||
            windSpeedKmh == null ||
            windAngleDeg == null ||
            driftAngleDeg == null
        ) {
            return null
        }
        if (trueAirspeedKmh <= 0.0) return null

        if (kotlin.math.abs(windAngleDeg) < 1e-9) {
            return trueAirspeedKmh + windSpeedKmh
        }
        if (kotlin.math.abs(windAngleDeg - 180.0) < 1e-9) {
            return trueAirspeedKmh - windSpeedKmh
        }

        val angleSin = kotlin.math.sin(Math.toRadians(windAngleDeg))
        if (kotlin.math.abs(angleSin) < 1e-9) return null

        val value = trueAirspeedKmh *
            kotlin.math.sin(Math.toRadians(180.0 - windAngleDeg - driftAngleDeg)) / angleSin
        if (value.isNaN() || value.isInfinite()) return null
        return round(value)
    }

    fun legTimeMinutes(distanceKm: Double?, groundSpeedKmh: Double?): Int? {
        if (distanceKm == null || groundSpeedKmh == null) return null
        if (distanceKm <= 0.0 || groundSpeedKmh <= 0.0) return null
        return ceil(distanceKm / groundSpeedKmh * 60.0 - 1e-6).toInt()
    }

    fun safeAltitude(obstacleHeightM: Double?, temperatureC: Double?): Double? {
        if (obstacleHeightM == null || temperatureC == null) return null
        val base = 100.0 + obstacleHeightM
        val corrected = base - ((temperatureC - 15.0) / 300.0) * base
        return ceil(corrected - 1e-6)
    }

    fun safeLevelTarget(
        obstacleHeightM: Double,
        temperatureC: Double,
        minPressureMmHg: Double
    ): Int {
        val base = 600.0 + obstacleHeightM + (760.0 - minPressureMmHg) * 11.0
        val corrected = base - ((temperatureC - 15.0) / 300.0) * base
        return ceil(corrected - 1e-6).toInt()
    }
}

