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
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.usecase.CheckUsagePermissionUseCase
import com.loadpredictor.domain.usecase.GetActiveBurnForecastUseCase
import com.loadpredictor.domain.usecase.GetActivePromoUseCase
import com.loadpredictor.domain.usecase.GetDailyUsageBreakdownUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Main ViewModel coordinating top-level app state: usage permission status,
 * active promo, real-time burn-rate forecast stream, and daily usage breakdown.
 *
 * Exposes immutable [StateFlow] in compliance with SKILL.md.
 */
class MainViewModel(
    private val checkUsagePermissionUseCase: CheckUsagePermissionUseCase,
    private val getActivePromoUseCase: GetActivePromoUseCase,
    private val getActiveBurnForecastUseCase: GetActiveBurnForecastUseCase,
    private val getDailyUsageBreakdownUseCase: GetDailyUsageBreakdownUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkPermission()
        observeActivePromo()
        observeBurnForecast()
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

    /**
     * Atomically refreshes both the live forecast and daily usage breakdown.
     */
    fun refresh() {
        viewModelScope.launch {
            checkPermission()
            val promo = _uiState.value.activePromo ?: try {
                getActivePromoUseCase().first()
            } catch (e: Exception) {
                null
            }

            if (promo == null) {
                _uiState.update { current ->
                    current.copy(
                        forecastResult = BurnForecastResult.NoActivePromo,
                        dailyUsageBreakdown = emptyList(),
                        isLoading = false
                    )
                }
                return@launch
            }

            val forecastResult = getActiveBurnForecastUseCase.execute(promo)
            val dailyBreakdown = if (getDailyUsageBreakdownUseCase != null) {
                try {
                    getDailyUsageBreakdownUseCase(promo)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            _uiState.update { current ->
                when (forecastResult) {
                    is BurnForecastResult.PermissionRequired -> current.copy(
                        isUsagePermissionGranted = false,
                        forecastResult = forecastResult,
                        dailyUsageBreakdown = dailyBreakdown,
                        isLoading = false
                    )
                    is BurnForecastResult.Success -> current.copy(
                        activePromo = forecastResult.forecast.promo,
                        forecastResult = forecastResult,
                        dailyUsageBreakdown = dailyBreakdown,
                        isLoading = false
                    )
                    else -> current.copy(
                        forecastResult = forecastResult,
                        dailyUsageBreakdown = dailyBreakdown,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeActivePromo() {
        getActivePromoUseCase()
            .onEach { promo ->
                val daily = if (promo != null && getDailyUsageBreakdownUseCase != null) {
                    try {
                        getDailyUsageBreakdownUseCase(promo)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                _uiState.update { current ->
                    current.copy(activePromo = promo, dailyUsageBreakdown = daily)
                }
            }
            .catch {
                _uiState.update { current ->
                    current.copy(activePromo = null, dailyUsageBreakdown = emptyList())
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeBurnForecast() {
        getActiveBurnForecastUseCase()
            .onEach { result ->
                _uiState.update { current ->
                    when (result) {
                        is BurnForecastResult.PermissionRequired -> {
                            current.copy(
                                isUsagePermissionGranted = false,
                                forecastResult = result,
                                isLoading = false
                            )
                        }
                        is BurnForecastResult.Success -> {
                            current.copy(
                                activePromo = result.forecast.promo,
                                forecastResult = result,
                                isLoading = false
                            )
                        }
                        else -> {
                            current.copy(
                                forecastResult = result,
                                isLoading = false
                            )
                        }
                    }
                }
            }
            .catch { throwable ->
                _uiState.update { current ->
                    current.copy(
                        forecastResult = BurnForecastResult.Error(
                            throwable.localizedMessage ?: "Unexpected forecasting error"
                        ),
                        isLoading = false
                    )
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
                    val getDailyUsageBreakdownUseCase = GetDailyUsageBreakdownUseCase(usageRepo)
                    val getActiveBurnForecastUseCase = GetActiveBurnForecastUseCase(
                        promoRepository = promoRepo,
                        usageRepository = usageRepo,
                        getDailyUsageBreakdownUseCase = getDailyUsageBreakdownUseCase
                    )

                    return MainViewModel(
                        checkUsagePermissionUseCase = checkPermissionUseCase,
                        getActivePromoUseCase = getActivePromoUseCase,
                        getActiveBurnForecastUseCase = getActiveBurnForecastUseCase,
                        getDailyUsageBreakdownUseCase = getDailyUsageBreakdownUseCase
                    ) as T
                }
            }
    }
}
