package com.jdluu.flexinsight.data.sync

import com.jdluu.flexinsight.core.errors.Result

/**
 * Narrow abstraction over the remote (Hevy) sync pipeline.
 * Lets sync orchestration run against any source that can pull all remote data,
 * without coupling to the full FlexRepository surface.
 */
interface HevySyncSource {
    suspend fun syncAll(): Result<Unit>
}
