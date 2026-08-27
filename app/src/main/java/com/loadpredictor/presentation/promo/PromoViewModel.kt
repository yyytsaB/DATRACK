package com.loadpredictor.presentation.promo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.local.NotificationPreferencesDataSource
import com.loadpredictor.data.repository.PromoRepositoryImpl
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.usecase.GetActivePromoUseCase
import com.loadpredictor.domain.usecase.SavePromoUseCase
import com.loadpredictor.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PromoUiState(
    val promos: List<Promo> = emptyList(),
    val activePromo: Promo? = null,
    val activeForecast: com.loadpredictor.domain.model.BurnForecast? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * ViewModel managing promo configuration and selection state.
 */
class PromoViewModel(
    private val promoRepository: PromoRepository,
    private val savePromoUseCase: SavePromoUseCase,
    private val getActivePromoUseCase: GetActivePromoUseCase,
    private val getActiveBurnForecastUseCase: com.loadpredictor.domain.usecase.GetActiveBurnForecastUseCase? = null,
    private val notificationPreferences: NotificationPreferencesDataSource? = null,
    private val onPromoMutated: (() -> Unit)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromoUiState())
    val uiState: StateFlow<PromoUiState> = _uiState.asStateFlow()

    init {
        observePromos()
        observeActivePromo()
        observeActiveBurnForecast()
    }

    private fun observeActiveBurnForecast() {
        getActiveBurnForecastUseCase?.invoke()
            ?.onEach { result ->
                _uiState.update { current ->
                    when (result) {
                        is com.loadpredictor.domain.model.BurnForecastResult.Success -> {
                            current.copy(activeForecast = result.forecast)
                        }
                        else -> current.copy(activeForecast = null)
                    }
                }
            }
            ?.catch {
                _uiState.update { current -> current.copy(activeForecast = null) }
            }
            ?.launchIn(viewModelScope)
    }

    private fun observePromos() {
        promoRepository.getAllPromos()
            .onEach { list ->
                _uiState.update { current ->
                    current.copy(promos = list, isLoading = false)
                }
            }
            .catch { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = error.localizedMessage, isLoading = false)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeActivePromo() {
        getActivePromoUseCase()
            .onEach { active ->
                _uiState.update { current ->
                    current.copy(activePromo = active)
                }
            }
            .launchIn(viewModelScope)
    }

    fun savePromo(
        name: String,
        allowanceBytes: Long,
        startTimestamp: Long,
        expirationTimestamp: Long?,
        initialUsageOffsetBytes: Long = 0L,
        simSlot: SimSlot = SimSlot.SIM_1,
        isActive: Boolean = true,
        id: Long = 0L,
        existingPromo: Promo? = null,
        onSuccess: (Long) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val promo = Promo(
                    id = id,
                    name = name,
                    totalAllowanceBytes = allowanceBytes,
                    startTimestamp = startTimestamp,
                    expirationTimestamp = expirationTimestamp,
                    initialUsageOffsetBytes = initialUsageOffsetBytes,
                    simSlot = simSlot,
                    isActive = isActive,
                    lastActiveBurnRate = null,
                    lastSyncDataUsedBytes = existingPromo?.lastSyncDataUsedBytes ?: 0L,
                    lastSyncTimestamp = existingPromo?.lastSyncTimestamp ?: 0L
                )
                val savedId = savePromoUseCase(promo)
                notificationPreferences?.clearThresholdsForPromo(savedId)
                onPromoMutated?.invoke()
                onSuccess(savedId)
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Failed to save promo"
                _uiState.update { it.copy(errorMessage = msg) }
                onError(msg)
            }
        }
    }

    fun selectActivePromo(promoId: Long) {
        viewModelScope.launch {
            promoRepository.setActivePromo(promoId)
            onPromoMutated?.invoke()
        }
    }

    fun deletePromo(promo: Promo) {
        viewModelScope.launch {
            notificationPreferences?.clearThresholdsForPromo(promo.id)
            promoRepository.deletePromo(promo)
            onPromoMutated?.invoke()
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val database = AppDatabase.getInstance(appContext)
                    val promoRepo = PromoRepositoryImpl(database.promoDao())
                    val savePromoUseCase = SavePromoUseCase(promoRepo)
                    val getActivePromoUseCase = GetActivePromoUseCase(promoRepo)
                    val notificationPrefs = NotificationPreferencesDataSource(appContext)

                    val usageHelper = com.loadpredictor.data.stats.UsageAccessHelper(appContext)
                    val networkStatsDataSource = com.loadpredictor.data.stats.NetworkStatsDataSource(appContext)
                    val usageRepo = com.loadpredictor.data.repository.UsageRepositoryImpl(usageHelper, networkStatsDataSource)
                    val getActiveBurnForecastUseCase = com.loadpredictor.domain.usecase.GetActiveBurnForecastUseCase(promoRepo, usageRepo)

                    return PromoViewModel(
                        promoRepository = promoRepo,
                        savePromoUseCase = savePromoUseCase,
                        getActivePromoUseCase = getActivePromoUseCase,
                        getActiveBurnForecastUseCase = getActiveBurnForecastUseCase,
                        notificationPreferences = notificationPrefs,
                        onPromoMutated = {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                try {
                                    com.loadpredictor.presentation.widget.WidgetSyncHelper.syncWidgetState(
                                        context = appContext,
                                        promoRepository = promoRepo,
                                        usageRepository = usageRepo
                                    )
                                } catch (_: Throwable) {}
                            }
                            WorkManagerScheduler.enqueueImmediateSync(appContext)
                        }
                    ) as T
                }
            }
    }
}

