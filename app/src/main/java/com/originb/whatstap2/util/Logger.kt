package com.originb.whatstap2.util

import android.util.Log
import com.originb.whatstap2.BuildConfig

object Logger {
    private const val TAG_PREFIX = "WhatsTap2"
    
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX:$tag", message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e("$TAG_PREFIX:$tag", message, throwable)
        } else {
            // In production, could send to crash reporting service
            // Firebase Crashlytics, Sentry, etc.
        }
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.w("$TAG_PREFIX:$tag", message, throwable)
        }
    }

    // Optional: Add structured logging for specific events
    fun logContactSync(insertedCount: Int, updatedCount: Int) {
        d("ContactSync", "Inserted: $insertedCount, Updated: $updatedCount")
    }
}

