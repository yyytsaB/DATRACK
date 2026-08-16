package com.loadpredictor.domain.model

/**
 * Represents the physical or virtual SIM slot context.
 * In v1, dual-SIM support is a manual toggle between two tracked promo contexts.
 */
enum class SimSlot {
    SIM_1,
    SIM_2;

    val displayName: String
        get() = when (this) {
            SIM_1 -> "SIM 1"
            SIM_2 -> "SIM 2"
        }
}
