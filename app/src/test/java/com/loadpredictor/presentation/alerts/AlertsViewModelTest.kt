package com.loadpredictor.presentation.alerts

import android.content.Context
import com.loadpredictor.data.local.AlertPreferencesDataSource
import com.loadpredictor.data.notification.NotificationHelper
import com.loadpredictor.domain.model.AlertPreferences
import com.loadpredictor.worker.WorkManagerScheduler
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val alertPreferencesDataSource: AlertPreferencesDataSource = mockk(relaxed = true)
    private val notificationHelper: NotificationHelper = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    private val preferencesFlow = MutableStateFlow(AlertPreferences())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(WorkManagerScheduler)
        every { WorkManagerScheduler.enqueueImmediateSync(any()) } returns Unit
        every { alertPreferencesDataSource.alertPreferencesFlow } returns preferencesFlow
        every { notificationHelper.canPostNotifications() } returns true
    }

    @After
    fun tearDown() {
        unmockkObject(WorkManagerScheduler)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState reflects default preferences and permission state`() = runTest {
        val viewModel = AlertsViewModel(alertPreferencesDataSource, notificationHelper, context)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.is50Enabled)
        assertTrue(state.is80Enabled)
        assertTrue(state.is90Enabled)
        assertTrue(state.isPrematureEnabled)
        assertTrue(state.hasNotificationPermission)
    }

    @Test
    fun `onToggle50Alert updates preferences and triggers immediate sync`() = runTest {
        val viewModel = AlertsViewModel(alertPreferencesDataSource, notificationHelper, context)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onToggle50Alert(false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            alertPreferencesDataSource.set50AlertEnabled(false)
        }
        verify(exactly = 1) {
            WorkManagerScheduler.enqueueImmediateSync(context)
        }
    }

    @Test
    fun `onToggle80Alert updates preferences and triggers immediate sync`() = runTest {
        val viewModel = AlertsViewModel(alertPreferencesDataSource, notificationHelper, context)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onToggle80Alert(false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            alertPreferencesDataSource.set80AlertEnabled(false)
        }
        verify(exactly = 1) {
            WorkManagerScheduler.enqueueImmediateSync(context)
        }
    }

    @Test
    fun `onToggle90Alert updates preferences and triggers immediate sync`() = runTest {
        val viewModel = AlertsViewModel(alertPreferencesDataSource, notificationHelper, context)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onToggle90Alert(false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            alertPreferencesDataSource.set90AlertEnabled(false)
        }
        verify(exactly = 1) {
            WorkManagerScheduler.enqueueImmediateSync(context)
        }
    }

    @Test
    fun `onTogglePrematureAlert updates preferences and triggers immediate sync`() = runTest {
        val viewModel = AlertsViewModel(alertPreferencesDataSource, notificationHelper, context)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onTogglePrematureAlert(false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            alertPreferencesDataSource.setPrematureAlertEnabled(false)
        }
        verify(exactly = 1) {
            WorkManagerScheduler.enqueueImmediateSync(context)
        }
    }

    @Test
    fun `refreshPermissionState updates permission status when changed`() = runTest {
        val viewModel = AlertsViewModel(alertPreferencesDataSource, notificationHelper, context)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasNotificationPermission)

        every { notificationHelper.canPostNotifications() } returns false
        viewModel.refreshPermissionState()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasNotificationPermission)
    }
}
