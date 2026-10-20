package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.time.DefaultTimeProvider
import com.loadpredictor.domain.time.TimeProvider

/**
 * UseCase to insert or update a promo configuration.
 * Validates that promo startTimestamp is not in the future using the injected [TimeProvider].
 * Automatically activates the saved promo if configured as active.
 */
class SavePromoUseCase(
    private val promoRepository: PromoRepository,
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {
    suspend operator fun invoke(promo: Promo): Long {
        require(promo.startTimestamp <= timeProvider.currentTimeMillis()) {
            "Promo start timestamp cannot be in the future"
        }

        return promoRepository.saveConfiguredPromo(promo)
    }
}
