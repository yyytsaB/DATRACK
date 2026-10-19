package com.loadpredictor.presentation.history

import android.content.Context
import androidx.lifecycle.*
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.repository.*
import com.loadpredictor.data.stats.*
import com.loadpredictor.domain.model.*
import com.loadpredictor.domain.usecase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class HistoryViewModel(
    private val getActivePromoUseCase: GetActivePromoUseCase,
    private val getDailyUsageBreakdownUseCase: GetDailyUsageBreakdownUseCase,
    private val getUsagePatternInsightUseCase: GetUsagePatternInsightUseCase = GetUsagePatternInsightUseCase()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    init {
        getActivePromoUseCase().distinctUntilChangedBy { promo -> promo?.copy(lastActiveBurnRate = null, lastSyncDataUsedBytes = 0, lastSyncTimestamp = 0) }
            .onEach { promo ->
                loadJob?.cancel()
                _uiState.update { it.copy(activePromo = promo, dailyBuckets = emptyList(), selectedBucketTimestamp = null) }
                refresh()
            }.catch { error -> _uiState.update { it.copy(errorMessage = error.localizedMessage, isLoading = false) } }
            .launchIn(viewModelScope)
    }
    fun setTimeRange(range: HistoryTimeRange) {
        if (_uiState.value.selectedRange == range) return
        _uiState.update { it.copy(selectedRange = range, selectedBucketTimestamp = null) }
        refresh()
    }
    fun selectBucket(timestamp: Long?) { _uiState.update { it.copy(selectedBucketTimestamp = timestamp) } }
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val promo = _uiState.value.activePromo ?: getActivePromoUseCase().first()
            if (promo == null) {
                _uiState.update { it.copy(activePromo = null, dailyBuckets = emptyList(), selectedBucketTimestamp = null,
                    patternInsight = UsagePatternInsight.InsufficientData, errorMessage = null, isLoading = false) }
                return@launch
            }
            val range = _uiState.value.selectedRange
            _uiState.update { it.copy(isLoading = true) }
            try {
                val buckets = getDailyUsageBreakdownUseCase(promo, timeRange = range)
                _uiState.update { current -> current.copy(activePromo = promo, dailyBuckets = buckets,
                    selectedBucketTimestamp = current.selectedBucketTimestamp?.takeIf { selected -> buckets.any { it.startTimestamp == selected } }
                        ?: buckets.lastOrNull()?.startTimestamp,
                    patternInsight = getUsagePatternInsightUseCase(buckets), isLoading = false, errorMessage = null) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(errorMessage = "Usage is unavailable. Check Usage Access and try again.", isLoading = false) }
            }
        }
    }
    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext
                val promo = PromoRepositoryImpl(AppDatabase.getInstance(app).promoDao())
                val usage = UsageRepositoryImpl(UsageAccessHelper(app), NetworkStatsDataSource(app))
                return HistoryViewModel(GetActivePromoUseCase(promo), GetDailyUsageBreakdownUseCase(usage)) as T
            }
        }
    }
}
