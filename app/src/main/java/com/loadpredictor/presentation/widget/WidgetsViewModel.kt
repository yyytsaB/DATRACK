package com.loadpredictor.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.repository.PromoRepositoryImpl
import com.loadpredictor.data.repository.UsageRepositoryImpl
import com.loadpredictor.data.stats.NetworkStatsDataSource
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.model.BurnForecastResult
import com.loadpredictor.domain.usecase.GetActiveBurnForecastUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.loadpredictor.domain.usecase.GetActivePromoUseCase
import kotlinx.coroutines.flow.firstOrNull

/**
 * ViewModel managing active forecast observation and home screen widget pinning requests.
 */
class WidgetsViewModel(
    private val getActivePromoUseCase: GetActivePromoUseCase,
    private val getActiveBurnForecastUseCase: GetActiveBurnForecastUseCase,
    private val context: Context
) : ViewModel() {

    private val isPinSupported: Boolean
        get() = try {
            val manager = AppWidgetManager.getInstance(context)
            manager != null && manager.isRequestPinAppWidgetSupported
        } catch (e: Throwable) {
            false
        }

    private val _uiState = MutableStateFlow(
        WidgetsUiState(
            isPinSupported = isPinSupported,
            isLoading = true
        )
    )
    val uiState: StateFlow<WidgetsUiState> = _uiState.asStateFlow()

    init {
        observeBurnForecast()
    }

    private fun observeBurnForecast() {
        getActiveBurnForecastUseCase()
            .onEach { result ->
                _uiState.update { current ->
                    when (result) {
                        is BurnForecastResult.Success -> current.copy(
                            activeForecast = result.forecast,
                            isLoading = false
                        )
                        else -> current.copy(
                            activeForecast = null,
                            isLoading = false
                        )
                    }
                }
            }
            .catch {
                _uiState.update { current ->
                    current.copy(activeForecast = null, isLoading = false)
                }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val promo = getActivePromoUseCase().firstOrNull()
                val result = getActiveBurnForecastUseCase.execute(promo)
                _uiState.update { current ->
                    when (result) {
                        is BurnForecastResult.Success -> current.copy(
                            activeForecast = result.forecast,
                            isLoading = false
                        )
                        else -> current.copy(
                            activeForecast = null,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(activeForecast = null, isLoading = false)
                }
            }
        }
    }

    /**
     * Requests the user's home screen launcher to pin the Load Predictor widget.
     *
     * @return True if the pin request was dispatched to the launcher; false if unsupported.
     */
    fun requestPinWidget(appContext: Context): Boolean {
        return try {
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = ComponentName(appContext, LoadPredictorWidgetReceiver::class.java)
                appWidgetManager.requestPinAppWidget(provider, null, null)
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
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
                    val getActivePromoUseCase = GetActivePromoUseCase(promoRepo)
                    val getActiveBurnForecastUseCase = GetActiveBurnForecastUseCase(promoRepo, usageRepo)

                    return WidgetsViewModel(
                        getActivePromoUseCase = getActivePromoUseCase,
                        getActiveBurnForecastUseCase = getActiveBurnForecastUseCase,
                        context = appContext
                    ) as T
                }
            }
    }
}
