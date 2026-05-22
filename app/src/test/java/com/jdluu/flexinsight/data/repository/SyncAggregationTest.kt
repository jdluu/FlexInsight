package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncAggregationTest {

    @Test
    fun mergeSyncErrors_returnsNullWhenNoErrors() {
        assertNull(FlexRepositoryImpl.mergeSyncErrors(emptyList()))
    }

    @Test
    fun mergeSyncErrors_singleErrorUsesOriginalMessage() {
        val error = ApiError.NetworkError.ConnectionError()
        val result = FlexRepositoryImpl.mergeSyncErrors(listOf(error))

        assertTrue(result is Result.Error)
        assertEquals("Unable to connect to server", (result as Result.Error).error.message)
    }

    @Test
    fun mergeSyncErrors_multipleErrorsJoined() {
        val result = FlexRepositoryImpl.mergeSyncErrors(
            listOf(
                ApiError.NetworkError.Unknown(),
                ApiError.ServerError.InternalServerError
            )
        )

        assertTrue(result is Result.Error)
        val message = (result as Result.Error).error.message!!
        assertTrue(message.contains("Sync failed:"))
        assertTrue(message.contains("Unknown network error"))
        assertTrue(message.contains("Internal server error"))
    }
}
