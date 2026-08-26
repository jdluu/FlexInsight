package com.jdluu.flexinsight.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.jdluu.flexinsight.TestApplication
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.fakes.ScriptableFlexRepository
import com.jdluu.flexinsight.fakes.resetForTests
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class PRListViewModelTest {

    private lateinit var repository: ScriptableFlexRepository
    private lateinit var userPreferences: UserPreferencesManager

    @Before
    fun setUp() {

        repository = ScriptableFlexRepository()
        repository.prsWithDetails = listOf(
            com.jdluu.flexinsight.data.model.PRDetails("Bench Press", 1000L, "Chest", 100.0, "w1", "s1"),
            com.jdluu.flexinsight.data.model.PRDetails("Squat", 2000L, "Legs", 140.0, "w2", "s2")
        )
        userPreferences = UserPreferencesManager(ApplicationProvider.getApplicationContext<Context>())
        kotlinx.coroutines.runBlocking { userPreferences.resetForTests() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun buildViewModel(): PRListViewModel =
        PRListViewModel(repository, userPreferences)

    @Test
    fun `init loads all personal records`() = runVmTest {
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState == LoadingState.Success }
            assertEquals(2, state.prs.size)
            assertEquals("Bench Press", state.prs.first().exerciseName)
            assertNull(state.error)
            assertEquals("Imperial", state.units)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init surfaces error state when records cannot be read`() = runVmTest {
        repository.prsError = IOException("pr boom")
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            val state = nextMatching { it.loadingState is LoadingState.Error }
            assertTrue(state.error is UiError.Network)
            assertEquals("Unable to connect to server", state.error?.message)
            assertTrue(state.prs.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `units preference changes are reflected`() = runVmTest {
        val viewModel = buildViewModel()
        viewModel.uiState.test {
            nextMatching { it.loadingState == LoadingState.Success }
        }

        userPreferences.setUnits("Metric")

        viewModel.uiState.test {
            nextMatching { it.units == "Metric" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadPRs refresh picks up new records`() = runVmTest {
        val viewModel = buildViewModel()
        viewModel.uiState.test {
            nextMatching { it.loadingState == LoadingState.Success }
            cancelAndIgnoreRemainingEvents()
        }

        repository.prsWithDetails += com.jdluu.flexinsight.data.model.PRDetails(
            "Deadlift", 3000L, "Back", 180.0, "w3", "s3"
        )
        viewModel.loadPRs()

        viewModel.uiState.test {
            val state = nextMatching { it.prs.size == 3 && it.loadingState == LoadingState.Success }
            assertEquals("Deadlift", state.prs.last().exerciseName)
            cancelAndIgnoreRemainingEvents()
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
