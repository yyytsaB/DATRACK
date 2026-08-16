package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.repository.UsageRepository

/**
 * UseCase to check whether Usage Access (PACKAGE_USAGE_STATS) is currently granted.
 */
class CheckUsagePermissionUseCase(
    private val usageRepository: UsageRepository
) {
    operator fun invoke(): Boolean {
        return usageRepository.hasUsageAccess()
    }
}
