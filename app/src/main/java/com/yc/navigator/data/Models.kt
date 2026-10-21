package com.yc.navigator.data

import kotlinx.serialization.Serializable

@Serializable
data class Pilot(
    val id: Long,
    val name: String
)

@Serializable
enum class PilotRole {
    COMMANDER,
    SECOND_PILOT,
    DUTY_NAVIGATOR,
    SQUADRON_NAVIGATOR
}

@Serializable
data class PilotAssignments(
    val commanderId: Long? = null,
    val secondPilotId: Long? = null,
    val dutyNavigatorId: Long? = null,
    val squadronNavigatorId: Long? = null
) {
    fun idFor(role: PilotRole): Long? = when (role) {
        PilotRole.COMMANDER -> commanderId
        PilotRole.SECOND_PILOT -> secondPilotId
        PilotRole.DUTY_NAVIGATOR -> dutyNavigatorId
        PilotRole.SQUADRON_NAVIGATOR -> squadronNavigatorId
    }

    fun withRole(role: PilotRole, pilotId: Long?): PilotAssignments = when (role) {
        PilotRole.COMMANDER -> copy(commanderId = pilotId)
        PilotRole.SECOND_PILOT -> copy(secondPilotId = pilotId)
        PilotRole.DUTY_NAVIGATOR -> copy(dutyNavigatorId = pilotId)
        PilotRole.SQUADRON_NAVIGATOR -> copy(squadronNavigatorId = pilotId)
    }
}

@Serializable
data class WindWeather(
    val point: String = "",
    val heightM: Double? = null,
    val windDirectionTrue: Double? = null,
    val windSpeedKmh: Double? = null,
    val magneticDeclination: Double? = null,
    val surfaceWindDirection: Double? = null,
    val surfaceWindSpeedKmh: Double? = null,
    val obstacleHeightM: Double? = null,
    val pressureMmHg: Double? = null,
    val temperatureC: Double? = null,
    val minPressureMmHg: Double? = null,
    val safeLevelHeightM: Double? = null,
    val assignedLevelM: Double? = null,
    val visibilityKm: Double? = null,
    val decisionHeightM: Double? = null,
    val frictionCoefficient: Double? = null,
    val landingCourseMagnetic: Double? = null,
    val weatherValidHours: Int? = null,
    val weatherValidMinutes: Int? = null
)

@Serializable
data class SunEntry(
    val id: Long = 0,
    val point: String = "",
    val sunrise: String = "",
    val sunset: String = "",
    val sum: String = ""
)

@Serializable
data class Aerodrome(
    val name: String = "",
    val declination: Double? = null,
    val windSpeedKmh: Double? = null,
    val pressureMmHg: Double? = null,
    val temperatureC: Double? = null,
    val sahPercent: Double? = null,
    val massKg: Double? = null,
    val speedKmh: Double? = null,
    val approachSpeedKmh: Double? = null,
    val landingSpeedKmh: Double? = null,
    val runM: Double? = null,
    val landingRunM: Double? = null,
    val safeCircleHeightM: Double? = null
)

@Serializable
data class Leg(
    val id: Long = 0,
    val reliefHeightM: Double? = null,
    val obstacleHeightM: Double? = null,
    val route: String = "",
    val magneticTrack: Double? = null,
    val trueAirspeedKmh: Double? = null,
    val distanceKm: Double? = null,
    val fuelActualKg: Double? = null,
    val calcTimeMinutes: Int? = null,
    val actualTimeMinutes: Int? = null,
    val flightLevelM: Double? = null,
    val notes: String = ""
)

@Serializable
data class FuelParams(
    val groundKg: Double? = null,
    val takeoffLandingKg: Double? = null,
    val reserveKg: Double? = null,
    val density: Double? = null,
    val hourlyConsumptionKg: Double? = null,
    val landingTimeMinutes: Int? = null
)

@Serializable
data class ActualFlight(
    val actualTimeMinutes: Int? = null,
    val nightTimeMinutes: Int? = null,
    val goAroundDistanceKm: Double? = null,
    val goAroundFuelKg: Double? = null,
    val diversionDistanceKm: Double? = null,
    val diversionFuelKg: Double? = null
)

@Serializable
data class Journal(
    val id: Long,
    val number: String = "",
    val dateIso: String = "",
    val unit: String = "",
    val aircraftType: String = "",
    val aircraftNumber: String = "",
    val pilots: PilotAssignments = PilotAssignments(),
    val windWeather: WindWeather = WindWeather(),
    val sunEntries: List<SunEntry> = emptyList(),
    val departure: Aerodrome = Aerodrome(),
    val arrival: Aerodrome = Aerodrome(),
    val legs: List<Leg> = emptyList(),
    val takeoffTimeMinutes: Int? = null,
    val landingTimeMinutes: Int? = null,
    val landingPoint: String = "",
    val fuel: FuelParams = FuelParams(),
    val actual: ActualFlight = ActualFlight(),
    val notes: String = "",
    val preflightDateIso: String = "",
    val flightRulesIfr: Boolean = true,
    val updatedAt: Long = 0L
)

@Serializable
enum class UnitMode {
    AUTO,
    RU,
    INTERNATIONAL
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
data class AppSettings(
    val languageTag: String = "ru",
    val unitMode: UnitMode = UnitMode.AUTO,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

@Serializable
data class ExportBundle(
    val version: Int = 1,
    val journals: List<Journal>,
    val pilots: List<Pilot>,
    val settings: AppSettings
)
