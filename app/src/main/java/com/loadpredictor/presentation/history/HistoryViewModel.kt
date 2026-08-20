package com.loadpredictor.presentation.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.repository.PromoRepositoryImpl
import com.loadpredictor.data.repository.UsageRepositoryImpl
import com.loadpredictor.data.stats.NetworkStatsDataSource
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.model.HistoryTimeRange
import com.loadpredictor.domain.model.Promo
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
 * Feature-scoped ViewModel managing state, time-range querying, and interactive selection
 * for the Usage History analytics tab.
 */
class HistoryViewModel(
    private val getActivePromoUseCase: GetActivePromoUseCase,
    private val getDailyUsageBreakdownUseCase: GetDailyUsageBreakdownUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeActivePromo()
    }

    /**
     * Updates the active time-range filter (7D / 30D / Lifetime) and re-queries buckets.
     */
    fun setTimeRange(range: HistoryTimeRange) {
        if (_uiState.value.selectedRange == range) return
        _uiState.update { it.copy(selectedRange = range, isLoading = true) }
        loadBucketsForRange(range)
    }

    /**
     * Selects a specific daily bucket by timestamp for chart inspection and tooltip display.
     */
    fun selectBucket(timestamp: Long?) {
        _uiState.update { it.copy(selectedBucketTimestamp = timestamp) }
    }

    /**
     * Refreshes history buckets for the active promo and current time-range.
     * Preserves [selectedBucketTimestamp] across periodic 30-second live refresh ticker ticks.
     */
    fun refresh() {
        viewModelScope.launch {
            val promo = _uiState.value.activePromo ?: try {
                getActivePromoUseCase().first()
            } catch (e: Exception) {
                null
            }

            if (promo == null) {
                _uiState.update {
                    it.copy(
                        activePromo = null,
                        dailyBuckets = emptyList(),
                        selectedBucketTimestamp = null,
                        isLoading = false
                    )
                }
                return@launch
            }

            val buckets = try {
                getDailyUsageBreakdownUseCase(promo, timeRange = _uiState.value.selectedRange)
            } catch (e: Exception) {
                emptyList()
            }

            _uiState.update { current ->
                // Preserve selection if timestamp still exists in new buckets
                val preservedTimestamp = if (current.selectedBucketTimestamp != null &&
                    buckets.any { it.startTimestamp == current.selectedBucketTimestamp }
                ) {
                    current.selectedBucketTimestamp
                } else {
                    buckets.lastOrNull()?.startTimestamp
                }

                current.copy(
                    activePromo = promo,
                    dailyBuckets = buckets,
                    selectedBucketTimestamp = preservedTimestamp,
                    isLoading = false
                )
            }
        }
    }

    private fun observeActivePromo() {
        getActivePromoUseCase()
            .onEach { promo ->
                _uiState.update { it.copy(activePromo = promo) }
                if (promo != null) {
                    loadBucketsForRange(_uiState.value.selectedRange, promo)
                } else {
                    _uiState.update {
                        it.copy(
                            dailyBuckets = emptyList(),
                            selectedBucketTimestamp = null,
                            isLoading = false
                        )
                    }
                }
            }
            .catch {
                _uiState.update {
                    it.copy(
                        activePromo = null,
                        dailyBuckets = emptyList(),
                        selectedBucketTimestamp = null,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadBucketsForRange(
        range: HistoryTimeRange,
        currentPromo: Promo? = _uiState.value.activePromo
    ) {
        viewModelScope.launch {
            val promo = currentPromo ?: try {
                getActivePromoUseCase().first()
            } catch (e: Exception) {
                null
            }

            if (promo == null) {
                _uiState.update { it.copy(dailyBuckets = emptyList(), isLoading = false) }
                return@launch
            }

            val buckets = try {
                getDailyUsageBreakdownUseCase(promo, timeRange = range)
            } catch (e: Exception) {
                emptyList()
            }

            _uiState.update { current ->
                val preservedTimestamp = if (current.selectedBucketTimestamp != null &&
                    buckets.any { it.startTimestamp == current.selectedBucketTimestamp }
                ) {
                    current.selectedBucketTimestamp
                } else {
                    buckets.lastOrNull()?.startTimestamp
                }

                current.copy(
                    activePromo = promo,
                    dailyBuckets = buckets,
                    selectedBucketTimestamp = preservedTimestamp,
                    isLoading = false
                )
            }
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
                    val getDailyUsageBreakdownUseCase = GetDailyUsageBreakdownUseCase(usageRepo)

                    return HistoryViewModel(
                        getActivePromoUseCase = getActivePromoUseCase,
                        getDailyUsageBreakdownUseCase = getDailyUsageBreakdownUseCase
                    ) as T
                }
            }
    }
}
