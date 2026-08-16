package com.loadpredictor.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.repository.PromoRepositoryImpl
import com.loadpredictor.data.repository.UsageRepositoryImpl
import com.loadpredictor.data.stats.NetworkStatsDataSource
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.usecase.CheckUsagePermissionUseCase
import com.loadpredictor.domain.usecase.GetActivePromoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Main ViewModel coordinating top-level app state: usage permission status and active promo.
 *
 * Exposes immutable [StateFlow] in compliance with SKILL.md.
 */
class MainViewModel(
    private val checkUsagePermissionUseCase: CheckUsagePermissionUseCase,
    private val getActivePromoUseCase: GetActivePromoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkPermission()
        observeActivePromo()
    }

    /**
     * Checks current PACKAGE_USAGE_STATS permission state.
     */
    fun checkPermission() {
        val hasPermission = checkUsagePermissionUseCase()
        _uiState.update { current ->
            current.copy(
                isUsagePermissionGranted = hasPermission,
                isLoading = false
            )
        }
    }

    private fun observeActivePromo() {
        getActivePromoUseCase()
            .onEach { promo ->
                _uiState.update { current ->
                    current.copy(activePromo = promo)
                }
            }
            .catch {
                _uiState.update { current ->
                    current.copy(activePromo = null)
                }
            }
            .launchIn(viewModelScope)
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val database = AppDatabase.getInstance(appContext)
                    val promoRepo = PromoRepositoryImpl(database.promoDao())
                    val usageHelper = UsageAccessHelper(appContext)
                    val networkStatsDataSource = NetworkStatsDataSource(appContext)
                    val usageRepo = UsageRepositoryImpl(usageHelper, networkStatsDataSource)

                    val checkPermissionUseCase = CheckUsagePermissionUseCase(usageRepo)
                    val getActivePromoUseCase = GetActivePromoUseCase(promoRepo)

                    return MainViewModel(
                        checkUsagePermissionUseCase = checkPermissionUseCase,
                        getActivePromoUseCase = getActivePromoUseCase
                    ) as T
                }
            }
    }
}
