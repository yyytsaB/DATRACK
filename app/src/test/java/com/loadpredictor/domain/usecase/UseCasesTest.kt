package com.loadpredictor.domain.usecase

import com.loadpredictor.domain.model.Promo
import com.loadpredictor.domain.model.SimSlot
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.domain.repository.PromoRepository
import com.loadpredictor.domain.repository.UsageRepository
import com.loadpredictor.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCasesTest {

    private class FakeTimeProvider(var currentTime: Long = 1_000_000L) : TimeProvider {
        override fun currentTimeMillis(): Long = currentTime
    }

    private class FakePromoRepository : PromoRepository {
        private val promos = mutableListOf<Promo>()
        private val activePromoFlow = MutableStateFlow<Promo?>(null)
        private val allPromosFlow = MutableStateFlow<List<Promo>>(emptyList())

        override fun getActivePromo(): Flow<Promo?> = activePromoFlow
        override fun getAllPromos(): Flow<List<Promo>> = allPromosFlow
        override fun getPromoById(id: Long): Flow<Promo?> = MutableStateFlow(promos.find { it.id == id })
        override fun getActivePromoForSim(simSlot: SimSlot): Flow<Promo?> =
            MutableStateFlow(promos.find { it.simSlot == simSlot && it.isActive })

        override suspend fun insertPromo(promo: Promo): Long {
            val id = (promos.size + 1).toLong()
            val created = promo.copy(id = id)
            promos.add(created)
            allPromosFlow.value = promos.toList()
            if (created.isActive) {
                setActivePromo(id)
            }
            return id
        }

        override suspend fun updatePromo(promo: Promo) {
            val idx = promos.indexOfFirst { it.id == promo.id }
            if (idx != -1) {
                promos[idx] = promo
                allPromosFlow.value = promos.toList()
            }
        }

        override suspend fun deletePromo(promo: Promo) {
            promos.removeAll { it.id == promo.id }
            allPromosFlow.value = promos.toList()
            if (activePromoFlow.value?.id == promo.id) {
                activePromoFlow.value = null
            }
        }

        override suspend fun setActivePromo(id: Long) {
            val updated = promos.map {
                it.copy(isActive = (it.id == id))
            }
            promos.clear()
            promos.addAll(updated)
            activePromoFlow.value = promos.find { it.id == id }
            allPromosFlow.value = promos.toList()
        }
    }

    private class FakeUsageRepository(var permissionGranted: Boolean = false) : UsageRepository {
        override fun hasUsageAccess(): Boolean = permissionGranted
        override suspend fun queryMobileUsageBytes(startTime: Long, endTime: Long): Long = 1024L * 1024L * 50L
        override suspend fun queryDailyUsageBreakdown(startTime: Long, endTime: Long): List<UsageBucket> = emptyList()
    }

    @Test
    fun `CheckUsagePermissionUseCase returns current permission status`() {
        val repo = FakeUsageRepository(permissionGranted = false)
        val useCase = CheckUsagePermissionUseCase(repo)

        assertFalse(useCase())

        repo.permissionGranted = true
        assertTrue(useCase())
    }

    @Test
    fun `GetActivePromoUseCase and SavePromoUseCase manage active promo correctly`() = runTest {
        val repo = FakePromoRepository()
        val timeProvider = FakeTimeProvider(currentTime = 10_000L)
        val getActive = GetActivePromoUseCase(repo)
        val savePromo = SavePromoUseCase(repo, timeProvider)

        assertNull(getActive().first())

        val promo1 = Promo(
            name = "Smart GigaSurf 99",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = 1000L,
            expirationTimestamp = 5000L,
            simSlot = SimSlot.SIM_1,
            isActive = true
        )

        val id1 = savePromo(promo1)
        assertEquals(1L, id1)

        val active = getActive().first()
        assertEquals("Smart GigaSurf 99", active?.name)
        assertTrue(active?.isActive == true)

        val promo2 = Promo(
            name = "Smart Power All 99",
            totalAllowanceBytes = 8L * 1024L * 1024L * 1024L,
            startTimestamp = 2000L,
            expirationTimestamp = 8000L,
            simSlot = SimSlot.SIM_2,
            isActive = true
        )

        val id2 = savePromo(promo2)
        assertEquals(2L, id2)

        val newActive = getActive().first()
        assertEquals("Smart Power All 99", newActive?.name)
        assertEquals(2L, newActive?.id)
    }

    @Test
    fun `SavePromoUseCase rejects expiring promo with future start timestamp`() = runTest {
        val repo = FakePromoRepository()
        val timeProvider = FakeTimeProvider(currentTime = 500_000L)
        val savePromo = SavePromoUseCase(repo, timeProvider)

        val futurePromo = Promo(
            name = "Smart GigaSurf Future",
            totalAllowanceBytes = 2L * 1024L * 1024L * 1024L,
            startTimestamp = 600_000L, // in the future relative to 500_000L
            expirationTimestamp = 900_000L
        )

        var thrown: IllegalArgumentException? = null
        try {
            savePromo(futurePromo)
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertEquals("Promo start timestamp cannot be in the future", thrown?.message)
    }

    @Test
    fun `SavePromoUseCase rejects non-expiring promo with future start timestamp`() = runTest {
        val repo = FakePromoRepository()
        val timeProvider = FakeTimeProvider(currentTime = 500_000L)
        val savePromo = SavePromoUseCase(repo, timeProvider)

        val futurePromo = Promo(
            name = "Smart Magic Data Future",
            totalAllowanceBytes = 24L * 1024L * 1024L * 1024L,
            startTimestamp = 600_000L, // in the future relative to 500_000L
            expirationTimestamp = null
        )

        var thrown: IllegalArgumentException? = null
        try {
            savePromo(futurePromo)
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertEquals("Promo start timestamp cannot be in the future", thrown?.message)
    }

    @Test
    fun `SavePromoUseCase saves promo with initial offset and respects registration time without backdating`() = runTest {
        val repo = FakePromoRepository()
        val registrationTime = 1_700_000_000_000L
        val timeProvider = FakeTimeProvider(currentTime = registrationTime)
        val savePromo = SavePromoUseCase(repo, timeProvider)
        val getActive = GetActivePromoUseCase(repo)

        val totalAllowance = 24L * 1024L * 1024L * 1024L // 24 GB
        val remainingEntered = 17_520_000_000L // 17.52 GB
        val initialOffset = totalAllowance - remainingEntered

        val promo = Promo(
            name = "Magic Data+ 499",
            totalAllowanceBytes = totalAllowance,
            startTimestamp = registrationTime,
            expirationTimestamp = null,
            initialUsageOffsetBytes = initialOffset,
            isActive = true
        )

        val id = savePromo(promo)
        val saved = getActive().first()

        assertEquals(id, saved?.id)
        assertEquals(registrationTime, saved?.startTimestamp)
        assertEquals(initialOffset, saved?.initialUsageOffsetBytes)
    }
}
