package com.jdluu.flexinsight

import android.app.Application
import java.io.File
import java.util.UUID

/**
 * Minimal application for Robolectric unit tests (avoids Hilt/WorkManager init).
 *
 * Robolectric shares one temporary directory across the test run, so DataStore-backed
 * managers would otherwise reuse one file across test methods: values leak between
 * tests and a second active DataStore instance for the same file fails. Pointing
 * filesDir at a unique subdirectory per application instance (Robolectric creates one
 * per test method) isolates every method's storage.
 */
class TestApplication : Application() {
    private val isolatedFilesDir: File by lazy {
        File(super.getFilesDir(), "test-" + UUID.randomUUID()).apply { mkdirs() }
    }

    override fun getFilesDir(): File = isolatedFilesDir
}
