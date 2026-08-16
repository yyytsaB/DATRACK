package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.repository.PromoRepository
import kotlinx.coroutines.flow.Flow

/**
 * Single-responsibility UseCase to observe the currently active promo context.
 */
class GetActivePromoUseCase(
    private val promoRepository: PromoRepository
) {
    operator fun invoke(): Flow<Promo?> {
        return promoRepository.getActivePromo()
    }
}
