package com.yc.navigator.domain

data class FlightLevel(
    val level: Int,
    val meters: Int,
    val feet: Int
)

data class SafeLevels(
    val ifr: Int?,
    val vfr: Int?
)

object FlightLevels {

    private val ifr0to179 = listOf(
        FlightLevel(10, 300, 1000),
        FlightLevel(30, 900, 3000),
        FlightLevel(50, 1500, 5000),
        FlightLevel(70, 2150, 7000),
        FlightLevel(90, 2750, 9000),
        FlightLevel(110, 3350, 11000),
        FlightLevel(130, 3950, 13000),
        FlightLevel(150, 4550, 15000),
        FlightLevel(170, 5200, 17000),
        FlightLevel(190, 5800, 19000),
        FlightLevel(210, 6400, 21000),
        FlightLevel(230, 7000, 23000),
        FlightLevel(250, 7600, 25000),
        FlightLevel(270, 8250, 27000),
        FlightLevel(290, 8850, 29000),
        FlightLevel(310, 9450, 31000),
        FlightLevel(330, 10050, 33000),
        FlightLevel(350, 10650, 35000),
        FlightLevel(370, 11300, 37000),
        FlightLevel(390, 11900, 39000),
        FlightLevel(410, 12500, 41000),
        FlightLevel(450, 13700, 45000),
        FlightLevel(490, 14950, 49000)
    )

    private val ifr180to359 = listOf(
        FlightLevel(35, 1050, 3500),
        FlightLevel(55, 1700, 5500),
        FlightLevel(75, 2300, 7500),
        FlightLevel(95, 2900, 9500),
        FlightLevel(115, 3500, 11500),
        FlightLevel(135, 4100, 13500),
        FlightLevel(155, 4700, 15500),
        FlightLevel(175, 5350, 17500),
        FlightLevel(195, 5950, 19500),
        FlightLevel(215, 6550, 21500),
        FlightLevel(235, 7150, 23500),
        FlightLevel(255, 7750, 25500)
    )

    private val vfr0to179 = listOf(
        FlightLevel(20, 600, 2000),
        FlightLevel(40, 1200, 4000),
        FlightLevel(60, 1850, 6000),
        FlightLevel(80, 2450, 8000),
        FlightLevel(100, 3050, 10000),
        FlightLevel(120, 3650, 12000),
        FlightLevel(140, 4250, 14000),
        FlightLevel(160, 4900, 16000),
        FlightLevel(180, 5500, 18000),
        FlightLevel(200, 6100, 20000),
        FlightLevel(220, 6700, 22000),
        FlightLevel(240, 7300, 24000),
        FlightLevel(260, 7900, 26000),
        FlightLevel(280, 8550, 28000),
        FlightLevel(300, 9150, 30000),
        FlightLevel(320, 9750, 32000),
        FlightLevel(340, 10350, 34000),
        FlightLevel(360, 10950, 36000),
        FlightLevel(380, 11600, 38000),
        FlightLevel(400, 12200, 40000),
        FlightLevel(430, 13100, 43000),
        FlightLevel(470, 14350, 47000),
        FlightLevel(510, 15550, 51000)
    )

    private val vfr180to359 = listOf(
        FlightLevel(45, 1350, 4500),
        FlightLevel(65, 2000, 6500),
        FlightLevel(85, 2600, 8500),
        FlightLevel(105, 3200, 10500),
        FlightLevel(125, 3800, 12500),
        FlightLevel(145, 4400, 14500),
        FlightLevel(165, 5050, 16500),
        FlightLevel(185, 5650, 18500),
        FlightLevel(205, 6250, 20500),
        FlightLevel(225, 6850, 22500),
        FlightLevel(245, 7450, 24500),
        FlightLevel(265, 8100, 26500)
    )

    fun safeLevels(
        obstacleHeightM: Double?,
        temperatureC: Double?,
        minPressureMmHg: Double?,
        trueTrackDeg: Double?
    ): SafeLevels {
        if (obstacleHeightM == null) return SafeLevels(null, null)
        val temperature = temperatureC ?: 15.0
        val pressure = minPressureMmHg ?: 760.0
        val target = Navigation.safeLevelTarget(obstacleHeightM, temperature, pressure)

        // The Excel template uses the 0-179 columns of the "Эшелоны" sheet
        // for both IFR and VFR calculations. Keep that behaviour 1:1.
        return SafeLevels(
            ifr = ifr0to179.firstOrNull { it.meters >= target }?.level,
            vfr = vfr0to179.firstOrNull { it.meters >= target }?.level
        )
    }
}
