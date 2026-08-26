package com.jdluu.flexinsight.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.jdluu.flexinsight.TestApplication
import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.core.network.NetworkState
import com.jdluu.flexinsight.data.model.ProfileInfo
import com.jdluu.flexinsight.data.preferences.SyncPreferencesManager
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.data.sync.SyncCoordinator
import com.jdluu.flexinsight.domain.ai.AiContextProvider
import com.jdluu.flexinsight.domain.usecase.BuildAiContextUseCase
import com.jdluu.flexinsight.domain.usecase.CalculateTrainingLoadUseCase
import com.jdluu.flexinsight.domain.usecase.ExportCoachReportUseCase
import com.jdluu.flexinsight.fakes.FakeWorkoutRepository
import com.jdluu.flexinsight.fakes.OneShotFakeAiClient
import com.jdluu.flexinsight.fakes.ScriptableApiKeyManager
import com.jdluu.flexinsight.fakes.ScriptableFlexRepository
import com.jdluu.flexinsight.fakes.healthConnectRepositoryStub
import com.jdluu.flexinsight.fakes.networkMonitorStub
import com.jdluu.flexinsight.fakes.resetForTests
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class SettingsViewModelTest {

    private lateinit var repository: ScriptableFlexRepository
    private lateinit var userPreferences: UserPreferencesManager
    private lateinit var syncPreferences: SyncPreferencesManager
    private lateinit var apiKeyManager: ScriptableApiKeyManager

    @Before
    fun setUp() {

        repository = ScriptableFlexRepository()
        repository.profileInfo = ProfileInfo(null, 1000L, false, 7, 30)

        val context = ApplicationProvider.getApplicationContext<Context>()
        userPreferences = UserPreferencesManager(context)
        syncPreferences = SyncPreferencesManager(context)
        apiKeyManager = ScriptableApiKeyManager()

        kotlinx.coroutines.runBlocking {
            userPreferences.resetForTests()
            syncPreferences.clearAllForTests()
        }

    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun buildViewModel(): SettingsViewModel = SettingsViewModel(
        repository = repository,
        userPreferencesManager = userPreferences,
        apiKeyManager = apiKeyManager.manager,
        networkMonitor = networkMonitorStub(NetworkState.Available).first,
        healthConnectRepository = healthConnectRepositoryStub(sdkAvailable = false),
        syncCoordinator = SyncCoordinator(FakeWorkoutRepository(), syncPreferences, healthConnectRepositoryStub()),
        exportCoachReportUseCase = ExportCoachReportUseCase(
            flexRepository = repository,
            buildAiContextUseCase = BuildAiContextUseCase(StaticProvider),
            aiClient = OneShotFakeAiClient(),
            calculateTrainingLoadUseCase = CalculateTrainingLoadUseCase(
                repository,
                healthConnectRepositoryStub()
            )
        )
    )

    // region Initialization

    @Test
    fun `init loads preference defaults and merges display name into profile`() = runVmTest {
        kotlinx.coroutines.runBlocking { userPreferences.setDisplayName("Jay") }

        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState == LoadingState.Success }
            assertEquals(5, state.weeklyGoal)
            assertEquals("System", state.theme)
            assertEquals("Imperial", state.units)
            assertTrue(state.notificationsEnabled)
            assertTrue(state.viewOnlyMode)
            assertFalse(state.forceAiEnable)
            assertFalse(state.healthConnectAvailable)
            assertFalse(state.healthConnectEnabled)
            assertFalse(state.healthConnectWriteEnabled)
            assertEquals(NetworkState.Available, state.networkState)
            assertEquals("Jay", state.profileInfo?.displayName)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `api key changes are reflected in ui state`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        apiKeyManager.keyFlow.value = "hevy-key-123"

        awaitUntil { viewModel.uiState.value.apiKey == "hevy-key-123" }
    }

    // endregion

    // region Actions

    @Test
    fun `updateWeeklyGoal persists and reflects immediately`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.updateWeeklyGoal(4)
        println("DBG-called updateWeeklyGoal(4)")

        awaitUntil {
            println("DBG weeklyGoal=" + viewModel.uiState.value.weeklyGoal + " loadingState=" + viewModel.uiState.value.loadingState)
            viewModel.uiState.value.weeklyGoal == 4
        }
        kotlinx.coroutines.runBlocking { assertEquals(4, userPreferences.getWeeklyGoal()) }
    }

    @Test
    fun `updateTheme and updateUnits persist and reflect`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.updateTheme("Dark")
        viewModel.updateUnits("Metric")

        awaitUntil { viewModel.uiState.value.theme == "Dark" && viewModel.uiState.value.units == "Metric" }
        kotlinx.coroutines.runBlocking {
            assertEquals("Dark", userPreferences.getTheme())
            assertEquals("Metric", userPreferences.getUnits())
        }
    }

    @Test
    fun `syncData success settles sync state and records last sync`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.syncData()

        viewModel.uiState.test {
            val state = nextMatching { it.syncState is LoadingState.Success }
            assertNull(state.syncError)
            cancelAndIgnoreRemainingEvents()
        }
        awaitUntil { syncPreferences.getLastSyncAt() != null }
    }

    @Test
    fun `syncData failure surfaces sync error without silent profile refresh`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)
        val profileBeforeSync = viewModel.uiState.value.profileInfo
        repository.profileInfo = ProfileInfo("NewName", 1000L, false, 9, 40)
        repository.syncResult = Result.Error(ApiError.AuthError.InvalidApiKey)

        viewModel.syncData()

        viewModel.uiState.test {
            val state = nextMatching { it.syncState is LoadingState.Error }
            assertNotNull(state.syncError)
            assertTrue(state.syncError is UiError.Auth)
            // Silent refresh is skipped on error: profile stays as loaded during init.
            assertEquals(profileBeforeSync?.displayName, state.profileInfo?.displayName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validateAndSaveApiKey rejects short keys with a message`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)
        var successCallback = false

        viewModel.validateAndSaveApiKey("short") { successCallback = true }

        viewModel.uiState.test {
            val state = nextMatching { it.apiKeyError != null }
            assertEquals("API key must be at least 10 characters", state.apiKeyError)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(apiKeyManager.savedKeys.isEmpty())
        assertFalse(successCallback)

        viewModel.clearApiKeyError()
        assertNull(viewModel.uiState.value.apiKeyError)
    }

    @Test
    fun `validateAndSaveApiKey stores valid keys and invokes callback`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)
        var successCallback = false

        viewModel.validateAndSaveApiKey("hevy-valid-key") { successCallback = true }

        awaitUntil { viewModel.uiState.value.apiKey == "hevy-valid-key" }
        assertEquals(listOf("hevy-valid-key"), apiKeyManager.savedKeys)
        assertTrue(successCallback)
        assertNull(viewModel.uiState.value.apiKeyError)
    }

    @Test
    fun `toggles mirror preferences into ui state`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.updateViewOnlyMode(false)
        viewModel.updateNotificationsEnabled(false)
        viewModel.updateForceAiEnable(true)

        awaitUntil {
            !viewModel.uiState.value.viewOnlyMode &&
                !viewModel.uiState.value.notificationsEnabled &&
                viewModel.uiState.value.forceAiEnable
        }
        kotlinx.coroutines.runBlocking { assertFalse(userPreferences.getViewOnlyMode()) }
    }

    @Test
    fun `health connect toggles reflect immediately`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.setHealthConnectEnabled(true)
        viewModel.setHealthConnectWriteEnabled(true)

        awaitUntil { viewModel.uiState.value.healthConnectWriteEnabled }
        assertTrue(viewModel.uiState.value.healthConnectEnabled)
    }

    @Test
    fun `updateDisplayName updates profile copy when profile exists`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.updateDisplayName("Jordan")

        awaitUntil { viewModel.uiState.value.profileInfo?.displayName == "Jordan" }
    }

    @Test
    fun `clearCache clears repository cache`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        viewModel.clearCache()

        assertEquals(1, repository.clearCacheCalls)
    }

    @Test
    fun `refresh silently reloads profile info`() = runVmTest {
        val viewModel = buildViewModel()
        awaitInitialLoad(testScheduler, viewModel)

        repository.profileInfo = ProfileInfo("Refreshed", 1000L, false, 12, 50)
        viewModel.refresh()

        viewModel.uiState.test {
            val state = nextMatching { it.profileInfo?.displayName == "Refreshed" }
            assertEquals(LoadingState.Success, state.loadingState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    private object StaticProvider : AiContextProvider {
        override suspend fun buildContext(userQuery: String?) =
            com.jdluu.flexinsight.data.ai.HevyAiDataAccessor.ContextSnapshot(
                text = "System Context",
                hasWorkoutData = true,
                hasApiKey = true,
                workoutCount = 1,
                usesLiveExerciseHistory = false
            )
    }

    /**
     * Resolves the init delay and waits for the initial load to finish so later user
     * actions cannot be overwritten by a late silent reload reading stale preferences.
     */
    private suspend fun awaitInitialLoad(
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
        viewModel: SettingsViewModel
    ) {
        scheduler.advanceUntilIdle()
        awaitUntil("initial load") { viewModel.uiState.value.loadingState == LoadingState.Success }
    }

    /** Polls with real time so DataStore-backed flows (real IO threads) can progress. */
    private suspend fun awaitUntil(
        label: String = "condition",
        timeoutMs: Long = 10_000,
        condition: suspend () -> Boolean
    ) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw AssertionError("$label not met within ${timeoutMs}ms")
            }
            withContext(Dispatchers.IO) { delay(10) }
        }
    }

    /** Consumes emissions until one satisfies [predicate], then discards the rest. */
    private suspend fun <T> TurbineTestContext<T>.nextMatching(predicate: (T) -> Boolean): T {
        var item = awaitItem()
        while (!predicate(item)) {
            item = awaitItem()
        }
        cancelAndIgnoreRemainingEvents()
        return item
    }

    /**
     * Runs [block] with Dispatchers.Main bound to an [UnconfinedTestDispatcher] sharing
     * this test's scheduler, so view-model coroutines (including init delays) resolve
     * on the same virtual clock the assertions observe.
     */
    private fun runVmTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) =
        kotlinx.coroutines.test.runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                block()
            } finally {
                Dispatchers.resetMain()
            }
        }

}
