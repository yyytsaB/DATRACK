package com.loadpredictor.presentation.alerts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loadpredictor.data.local.AlertPreferencesDataSource
import com.loadpredictor.data.notification.NotificationHelper
import com.loadpredictor.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing toggle states and background sync triggers for data burn notifications.
 */
class AlertsViewModel(
    private val alertPreferencesDataSource: AlertPreferencesDataSource,
    private val notificationHelper: NotificationHelper,
    private val context: Context
) : ViewModel() {

    private val _hasNotificationPermission = MutableStateFlow(notificationHelper.canPostNotifications())

    val uiState: StateFlow<AlertsUiState> = combine(
        alertPreferencesDataSource.alertPreferencesFlow,
        _hasNotificationPermission
    ) { alertPrefs, hasPermission ->
        AlertsUiState(
            is50Enabled = alertPrefs.is50Enabled,
            is80Enabled = alertPrefs.is80Enabled,
            is90Enabled = alertPrefs.is90Enabled,
            isPrematureEnabled = alertPrefs.isPrematureEnabled,
            hasNotificationPermission = hasPermission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlertsUiState(
            hasNotificationPermission = notificationHelper.canPostNotifications()
        )
    )

    fun refreshPermissionState() {
        _hasNotificationPermission.value = notificationHelper.canPostNotifications()
    }

    fun onToggle50Alert(enabled: Boolean) {
        viewModelScope.launch {
            alertPreferencesDataSource.set50AlertEnabled(enabled)
            WorkManagerScheduler.enqueueImmediateSync(context)
        }
    }

    fun onToggle80Alert(enabled: Boolean) {
        viewModelScope.launch {
            alertPreferencesDataSource.set80AlertEnabled(enabled)
            WorkManagerScheduler.enqueueImmediateSync(context)
        }
    }

    fun onToggle90Alert(enabled: Boolean) {
        viewModelScope.launch {
            alertPreferencesDataSource.set90AlertEnabled(enabled)
            WorkManagerScheduler.enqueueImmediateSync(context)
        }
    }

    fun onTogglePrematureAlert(enabled: Boolean) {
        viewModelScope.launch {
            alertPreferencesDataSource.setPrematureAlertEnabled(enabled)
            WorkManagerScheduler.enqueueImmediateSync(context)
        }
    }

    companion object {
        fun provideFactory(context: Context): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val alertPrefs = AlertPreferencesDataSource(context.applicationContext)
                    val notificationHelper = NotificationHelper(context.applicationContext)
                    return AlertsViewModel(
                        alertPreferencesDataSource = alertPrefs,
                        notificationHelper = notificationHelper,
                        context = context.applicationContext
                    ) as T
                }
            }
    }
}
