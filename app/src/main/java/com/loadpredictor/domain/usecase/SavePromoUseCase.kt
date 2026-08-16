package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.repository.PromoRepository

/**
 * UseCase to insert or update a promo configuration.
 * Automatically activates the saved promo if configured as active.
 */
class SavePromoUseCase(
    private val promoRepository: PromoRepository
) {
    suspend operator fun invoke(promo: Promo): Long {
        return if (promo.id == 0L) {
            val generatedId = promoRepository.insertPromo(promo)
            if (promo.isActive) {
                promoRepository.setActivePromo(generatedId)
            }
            generatedId
        } else {
            promoRepository.updatePromo(promo)
            if (promo.isActive) {
                promoRepository.setActivePromo(promo.id)
            }
            promo.id
        }
    }
}
