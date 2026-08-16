package com.loadpredictor.presentation.promo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.repository.PromoRepositoryImpl
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.usecase.GetActivePromoUseCase
import com.loadpredictor.domain.usecase.SavePromoUseCase
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
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * ViewModel managing promo configuration and selection state.
 */
class PromoViewModel(
    private val promoRepository: PromoRepository,
    private val savePromoUseCase: SavePromoUseCase,
    private val getActivePromoUseCase: GetActivePromoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromoUiState())
    val uiState: StateFlow<PromoUiState> = _uiState.asStateFlow()

    init {
        observePromos()
        observeActivePromo()
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
        simSlot: SimSlot = SimSlot.SIM_1,
        isActive: Boolean = true,
        id: Long = 0L,
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
                    simSlot = simSlot,
                    isActive = isActive
                )
                val savedId = savePromoUseCase(promo)
                onSuccess(savedId)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to save promo")
            }
        }
    }

    fun selectActivePromo(promoId: Long) {
        viewModelScope.launch {
            promoRepository.setActivePromo(promoId)
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
                    val savePromoUseCase = SavePromoUseCase(promoRepo)
                    val getActivePromoUseCase = GetActivePromoUseCase(promoRepo)

                    return PromoViewModel(
                        promoRepository = promoRepo,
                        savePromoUseCase = savePromoUseCase,
                        getActivePromoUseCase = getActivePromoUseCase
                    ) as T
                }
            }
    }
}
