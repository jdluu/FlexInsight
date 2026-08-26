package com.jdluu.flexinsight.ui.viewmodel

import app.cash.turbine.test
import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.ai.AiFeatureStatus
import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor
import com.jdluu.flexinsight.data.preferences.ApiKeyStatusSource
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.data.sync.ManualSyncScheduler
import com.jdluu.flexinsight.data.sync.SyncCompleteListener
import com.jdluu.flexinsight.domain.ai.AiContextProvider
import com.jdluu.flexinsight.domain.usecase.BuildAiContextUseCase
import com.jdluu.flexinsight.fakes.FakeAiClient
import com.jdluu.flexinsight.TestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class AITrainerViewModelTest {

    private val systemContextText =
        "System Context - Hevy Training Data (synced from Hevy API):\n- Total logged workouts: 7"

    private lateinit var aiClient: FakeAiClient
    private lateinit var contextProvider: FakeContextProvider
    private lateinit var flexRepository: FlexRepository
    private lateinit var apiKeySource: FakeApiKeySource
    private lateinit var syncScheduler: ManualSyncScheduler
    private lateinit var syncListener: SyncCompleteListener

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        aiClient = FakeAiClient()
        contextProvider = FakeContextProvider(defaultSnapshot())
        apiKeySource = FakeApiKeySource(hasKey = false)
        flexRepository = mockk(relaxed = true)
        coEvery { flexRepository.syncAllData() } returns Result.Error(ApiError.Unknown("offline"))
        syncScheduler = mockk()
        every { syncScheduler.syncNow() } returns Unit
        syncListener = mockk()
        coEvery { syncListener.onSyncComplete() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // region Initialization / readiness

    @Test
    fun `ready status prepares model and greets without syncing when api key missing`() = runTest {
        // Snapshot must mirror the no-key state so the greeting prompt takes the
        // "not connected" branch; the shared default fixture has a connected key.
        contextProvider.queued += defaultSnapshot().copy(hasWorkoutData = false, hasApiKey = false, workoutCount = 0)
        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.isAiAvailable)
        assertFalse(state.isPreparingModel)
        assertFalse(state.isSyncingHevyData)
        assertEquals(1, state.messages.size)
        val greeting = state.messages.single()
        assertEquals("ai", greeting.sender)
        assertEquals("Hello!", greeting.text)

        val prompt = aiClient.prompts.single()
        assertTrue(prompt.startsWith(systemContextText))
        assertTrue(prompt.contains("The user has not connected Hevy yet"))

        coVerify(exactly = 0) { flexRepository.syncAllData() }
    }

    @Test
    fun `ready status syncs hevy data before greeting when api key present`() = runTest {
        apiKeySource.hasKey = true
        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.hasHevyData)
        assertEquals(7, state.hevyWorkoutCount)
        assertFalse(state.isSyncingHevyData)
        assertEquals(1, state.messages.size)

        val prompt = aiClient.prompts.single()
        assertTrue(prompt.contains("Based on this user's real Hevy training data"))
    }

    @Test
    fun `prepare failure surfaces error bubble and disables availability`() = runTest {
        aiClient.prepareResult = Result.Error(ApiError.Unknown("model download failed"))
        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isAiAvailable)
        assertFalse(state.isPreparingModel)
        assertEquals("model download failed", state.aiStatusMessage)
        assertEquals("ai", state.messages.single().sender)
        assertEquals("model download failed", state.messages.single().text)
    }

    @Test
    fun `downloading status keeps preparing and informs user`() = runTest {
        aiClient.featureStatus = AiFeatureStatus.Downloading
        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isAiAvailable)
        assertTrue(state.isPreparingModel)
        assertEquals(
            "Gemini Nano is downloading. This may take a few minutes on first use.",
            state.aiStatusMessage
        )
        assertEquals(state.aiStatusMessage, state.messages.single().text)
    }

    @Test
    fun `unavailable status disables chat and blocks sending`() = runTest {
        aiClient.featureStatus = AiFeatureStatus.Unavailable
        val viewModel = buildViewModel()

        val initialState = viewModel.uiState.value
        assertFalse(initialState.isAiAvailable)
        assertFalse(initialState.isPreparingModel)
        assertTrue(
            initialState.aiStatusMessage!!.startsWith("On-device AI isn't available on this device.")
        )
        assertEquals(initialState.aiStatusMessage, initialState.messages.single().text)

        viewModel.sendMessage("hi")

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertTrue(aiClient.streamPrompts.isEmpty())
    }

    // endregion

    // region sendMessage streaming

    @Test
    fun `sendMessage streams chunks into assistant reply with system context history`() = runTest {
        val viewModel = buildViewModel()
        aiClient.streamChunks = listOf("Bench ", "progress ", "looks great")
        contextProvider.usesLiveForQueries = true

        viewModel.sendMessage("How's my bench?")

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size)
        assertEquals("user", state.messages[1].sender)
        assertEquals("How's my bench?", state.messages[1].text)
        assertEquals("ai", state.messages[2].sender)
        assertEquals("Bench progress looks great", state.messages[2].text)
        assertFalse(state.isTyping)
        assertNull(state.error)
        assertTrue(state.usesLiveExerciseHistory)
        assertEquals("How's my bench?", aiClient.streamPrompts.single())
        assertEquals("How's my bench?", contextProvider.queries.last())

        val history = aiClient.histories.last() // greeting used generateResponse; send used the stream
        assertEquals("user", history.first().first)
        assertTrue(history.first().second.startsWith("System Context:\n$systemContextText"))
        assertEquals(
            "model" to "Understood. I will coach using only this Hevy data and say when something is not available.",
            history[1]
        )
    }

    @Test
    fun `error chunk from stream sets error state without adding reply`() = runTest {
        val viewModel = buildViewModel()
        aiClient.streamChunks = listOf("Error: on-device model busy")

        viewModel.sendMessage("hello?")

        val state = viewModel.uiState.value
        assertEquals("Error: on-device model busy", state.error)
        assertFalse(state.isTyping)
        assertEquals(2, state.messages.size) // greeting + user message only
    }

    @Test
    fun `stream exception surfaces fallback assistant message`() = runTest {
        val viewModel = buildViewModel()
        aiClient.streamError = IllegalStateException("boom")

        viewModel.sendMessage("hello?")

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size)
        assertEquals("I'm having trouble thinking right now. Please try again.", state.messages[2].text)
        assertFalse(state.isTyping)
    }

    @Test
    fun `blank messages are ignored`() = runTest {
        val viewModel = buildViewModel()

        viewModel.sendMessage("   ")

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertTrue(aiClient.streamPrompts.isEmpty())
    }

    // endregion

    // region refreshHevySync (Turbine state emissions)

    @Test
    fun `refreshHevySync emits syncing then settled state and notifies listener`() = runTest {
        val viewModel = buildViewModel()
        apiKeySource.hasKey = true
        contextProvider.queued += defaultSnapshot().copy(workoutCount = 9)

        val gate = CompletableDeferred<Unit>()
        coEvery { flexRepository.syncAllData() } coAnswers { gate.await(); Result.Success(Unit) }

        viewModel.refreshHevySync()

        viewModel.uiState.test {
            val syncing = awaitItem()
            assertTrue(syncing.isSyncingHevyData)

            gate.complete(Unit)
            val settled = awaitItem()
            assertFalse(settled.isSyncingHevyData)
            assertEquals(9, settled.hevyWorkoutCount)
            assertTrue(settled.hasHevyData)
        }

        verify(exactly = 1) { syncScheduler.syncNow() }
        coVerify(exactly = 1) { syncListener.onSyncComplete() }
        assertNull(contextProvider.queries.last()) // manual refresh rebuilds context without a query
    }

    @Test
    fun `refreshHevySync skips when api key missing`() = runTest {
        val viewModel = buildViewModel()

        viewModel.refreshHevySync()

        verify(exactly = 0) { syncScheduler.syncNow() }
        coVerify(exactly = 0) { flexRepository.syncAllData() }
    }

    // endregion

    // region Fixtures

    private fun defaultSnapshot() = HevyAiDataAccessor.ContextSnapshot(
        text = systemContextText,
        hasWorkoutData = true,
        hasApiKey = true,
        workoutCount = 7,
        usesLiveExerciseHistory = false
    )

    private fun buildViewModel(): AITrainerViewModel = AITrainerViewModel(
        aiClient = aiClient,
        buildAiContextUseCase = BuildAiContextUseCase(contextProvider),
        flexRepository = flexRepository,
        apiKeyManager = apiKeySource,
        syncManager = syncScheduler,
        syncCoordinator = syncListener
    )

    private class FakeApiKeySource(var hasKey: Boolean) : ApiKeyStatusSource {
        override suspend fun hasApiKey(): Boolean = hasKey
    }

    /** Returns queued snapshots in order, then repeats [base]; flags live history for real queries. */
    private class FakeContextProvider(
        private val base: HevyAiDataAccessor.ContextSnapshot
    ) : AiContextProvider {
        val queries = mutableListOf<String?>()
        val queued = ArrayDeque<HevyAiDataAccessor.ContextSnapshot>()
        var usesLiveForQueries = false

        override suspend fun buildContext(userQuery: String?): HevyAiDataAccessor.ContextSnapshot {
            queries += userQuery
            val next = queued.removeFirstOrNull() ?: base
            return next.copy(usesLiveExerciseHistory = userQuery != null && usesLiveForQueries)
        }
    }

    // endregion
}
