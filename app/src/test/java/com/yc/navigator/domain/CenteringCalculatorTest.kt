package com.yc.navigator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CenteringCalculatorTest {

    @Test
    fun sample172NLoadingIsWithinLimits() {
        val result = calculateCentering(
            CenteringInput(
                fuelWeightLb = 240.0,
                pilotsWeightLb = 340.0,
                rearPassengersWeightLb = 170.0,
                baggage1WeightLb = 103.0,
                baggage2WeightLb = 0.0,
                fuelAllowanceLb = 7.0,
                category = CenteringCategory.NORMAL
            )
        )

        assertEquals(2307.0, result.rampWeightLb, 0.01)
        assertEquals(103.9, result.rampMomentK, 0.05)
        assertEquals(2300.0, result.takeoffWeightLb, 0.01)
        assertEquals(103.6, result.takeoffMomentK, 0.05)
        assertEquals(45.04, result.cgArmIn, 0.05)
        assertTrue(result.isOk)
    }

    @Test
    fun combinedBaggageLimitIsEnforced() {
        val result = calculateCentering(
            CenteringInput(
                fuelWeightLb = 120.0,
                pilotsWeightLb = 340.0,
                rearPassengersWeightLb = 0.0,
                baggage1WeightLb = 100.0,
                baggage2WeightLb = 30.0,
                fuelAllowanceLb = 7.0
            )
        )

        assertTrue(!result.baggageOk)
    }
}
