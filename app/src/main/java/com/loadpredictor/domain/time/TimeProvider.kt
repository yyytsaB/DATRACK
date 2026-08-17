package com.loadpredictor.domain.time

/**
 * Pure Kotlin interface abstracting system time for deterministic testing
 * and clock injection across domain models, use cases, and forecast engines,
 * per Engineering Rule #5.
 */
interface TimeProvider {
    fun currentTimeMillis(): Long
}

/**
 * Default implementation backed by the system wall clock.
 */
class DefaultTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
