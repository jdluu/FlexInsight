package com.example.flexinsight.core.logger

import android.util.Log
import com.example.flexinsight.BuildConfig

/**
 * Centralized logger for the application.
 * Wraps android.util.Log and enables logging only for debug builds.
 */
object AppLogger {
    private const val DEFAULT_TAG = "FlexInsight"

    fun d(message: String, tag: String = DEFAULT_TAG) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun i(message: String, tag: String = DEFAULT_TAG) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    fun w(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        // Errors might be logged even in release builds if we had Crashlytics, etc.
        // For now, keeping it consistent with debug check, or we could allow it.
        // Usually we want Errors in logcat if possible, but sensitive info is a risk.
        // Keeping checks for now.
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
}
