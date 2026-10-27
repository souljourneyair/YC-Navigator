package com.yc.navigator.domain

enum class CenteringCategory {
    NORMAL,
    UTILITY
}

data class CenteringInput(
    val fuelWeightLb: Double,
    val pilotsWeightLb: Double,
    val rearPassengersWeightLb: Double,
    val baggage1WeightLb: Double,
    val baggage2WeightLb: Double,
    val fuelAllowanceLb: Double,
    val category: CenteringCategory = CenteringCategory.NORMAL
)

data class CenteringResult(
    val rampWeightLb: Double,
    val rampMomentK: Double,
    val takeoffWeightLb: Double,
    val takeoffMomentK: Double,
    val cgArmIn: Double,
    val forwardLimitIn: Double,
    val aftLimitIn: Double,
    val baggageOk: Boolean,
    val weightOk: Boolean,
    val cgOk: Boolean
) {
    val isOk: Boolean
        get() = baggageOk && weightOk && cgOk
}

fun calculateCentering(input: CenteringInput): CenteringResult {
    val rampWeight = EMPTY_WEIGHT_LB +
        input.fuelWeightLb +
        input.pilotsWeightLb +
        input.rearPassengersWeightLb +
        input.baggage1WeightLb +
        input.baggage2WeightLb
    val rampMoment = EMPTY_MOMENT_K +
        input.fuelWeightLb * FUEL_ARM_IN / 1000.0 +
        input.pilotsWeightLb * FRONT_SEATS_ARM_IN / 1000.0 +
        input.rearPassengersWeightLb * REAR_SEATS_ARM_IN / 1000.0 +
        input.baggage1WeightLb * BAGGAGE1_ARM_IN / 1000.0 +
        input.baggage2WeightLb * BAGGAGE2_ARM_IN / 1000.0

    val allowance = input.fuelAllowanceLb.coerceAtLeast(0.0)
    val takeoffWeight = rampWeight - allowance
    val takeoffMoment = rampMoment - allowance * FUEL_ARM_IN / 1000.0
    val cgArm = if (takeoffWeight > 0.0) takeoffMoment * 1000.0 / takeoffWeight else 0.0

    val forwardLimit = when (input.category) {
        CenteringCategory.NORMAL -> if (takeoffWeight <= FORWARD_LIMIT_WEIGHT_LB) {
            FORWARD_LIMIT_LOW_CG_IN
        } else {
            FORWARD_LIMIT_LOW_CG_IN +
                (takeoffWeight - FORWARD_LIMIT_WEIGHT_LB) *
                (FORWARD_LIMIT_HIGH_CG_IN - FORWARD_LIMIT_LOW_CG_IN) /
                (NORMAL_MAX_TAKEOFF_WEIGHT_LB - FORWARD_LIMIT_WEIGHT_LB)
        }

        CenteringCategory.UTILITY -> UTILITY_FORWARD_LIMIT_CG_IN
    }
    val aftLimit = when (input.category) {
        CenteringCategory.NORMAL -> NORMAL_AFT_LIMIT_CG_IN
        CenteringCategory.UTILITY -> UTILITY_AFT_LIMIT_CG_IN
    }
    val maxWeight = when (input.category) {
        CenteringCategory.NORMAL -> NORMAL_MAX_TAKEOFF_WEIGHT_LB
        CenteringCategory.UTILITY -> UTILITY_MAX_TAKEOFF_WEIGHT_LB
    }

    val baggageOk = input.baggage1WeightLb <= BAGGAGE1_MAX_LB &&
        input.baggage2WeightLb <= BAGGAGE2_MAX_LB &&
        input.baggage1WeightLb + input.baggage2WeightLb <= BAGGAGE_COMBINED_MAX_LB
    val weightOk = takeoffWeight <= maxWeight
    val cgOk = cgArm >= forwardLimit && cgArm <= aftLimit

    return CenteringResult(
        rampWeightLb = rampWeight,
        rampMomentK = rampMoment,
        takeoffWeightLb = takeoffWeight,
        takeoffMomentK = takeoffMoment,
        cgArmIn = cgArm,
        forwardLimitIn = forwardLimit,
        aftLimitIn = aftLimit,
        baggageOk = baggageOk,
        weightOk = weightOk,
        cgOk = cgOk
    )
}

const val EMPTY_WEIGHT_LB = 1454.0
const val EMPTY_MOMENT_K = 57.6
const val FUEL_LB_PER_GALLON = 6.0
const val FUEL_ARM_IN = 48.0
const val FRONT_SEATS_ARM_IN = 37.0
const val REAR_SEATS_ARM_IN = 73.0
const val BAGGAGE1_ARM_IN = 95.0
const val BAGGAGE2_ARM_IN = 123.0
const val BAGGAGE1_MAX_LB = 120.0
const val BAGGAGE2_MAX_LB = 50.0
const val BAGGAGE_COMBINED_MAX_LB = 120.0
const val NORMAL_MAX_TAKEOFF_WEIGHT_LB = 2300.0
const val UTILITY_MAX_TAKEOFF_WEIGHT_LB = 2000.0
const val FORWARD_LIMIT_WEIGHT_LB = 1950.0
const val FORWARD_LIMIT_LOW_CG_IN = 35.0
const val FORWARD_LIMIT_HIGH_CG_IN = 38.5
const val NORMAL_AFT_LIMIT_CG_IN = 47.3
const val UTILITY_FORWARD_LIMIT_CG_IN = 35.0
const val UTILITY_AFT_LIMIT_CG_IN = 40.5
