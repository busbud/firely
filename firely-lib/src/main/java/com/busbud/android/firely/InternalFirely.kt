/*
 * Copyright 2017 Busbud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.busbud.android.firely

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.util.concurrent.TimeUnit


private val LOG_TAG = Firely::class.simpleName
private const val SHARED_PREF_INITIAL_CHECK = "com.busbud.android.firely.initial_check"

class InternalFirely(context: Context, private val config: IFirelyConfig) {

    private val firebaseRemoteConfig: FirebaseRemoteConfig
    private val allPropsWithCurrentValue = mutableMapOf<String, String>()
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var debugMode = false

    init {
        FirebaseApp.initializeApp(context)
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        (context.applicationContext as Application).registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
                // Do Nothing
            }

            override fun onActivityStarted(activity: Activity) {
                // Do Nothing
            }

            override fun onActivityResumed(activity: Activity) {
                // Fetch the app while active
                fetch()
            }

            override fun onActivityPaused(activity: Activity) {
                if (debugMode) {
                    activateFetched()
                }
            }

            override fun onActivityStopped(activity: Activity) {
                // Do Nothing
            }

            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
                // Do Nothing
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (!debugMode && activity.isTaskRoot && activity.isFinishing) {
                    activateFetched()
                }
            }
        })


        config.allValues()
            .associate { it.key to it.default }
            .also { defaults ->
                firebaseRemoteConfig.setDefaultsAsync(defaults)
            }

        // Init tracking properties from FirebaseRemoteConfig for tracking
        updateAllTrackingProperties()
    }

    fun getString(name: String): String = firebaseRemoteConfig.getString(name)
    fun getBoolean(name: String) = firebaseRemoteConfig.getBoolean(name)
    fun getDouble(name: String): Double = firebaseRemoteConfig.getDouble(name)
    fun getLong(name: String): Long = firebaseRemoteConfig.getLong(name)

    fun getCurrentKnownValues(): Map<String, String> {
        return allPropsWithCurrentValue
    }

    fun setDebugMode(debugMode: Boolean) {
        this.debugMode = debugMode

        FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(
                if (debugMode) {
                    0L
                } else {
                    TimeUnit.HOURS.toSeconds(12)
                }
            )
            .build()
            .also {
                firebaseRemoteConfig.setConfigSettingsAsync(it)
            }
    }

    fun fetch() {

        var cacheExpiration = 7200L // 1 hour in seconds - 5 requests per hour limitation

        // Hack to be sure to force fetch the configuration at first launch
        if (!prefs.getBoolean(SHARED_PREF_INITIAL_CHECK, false)) {
            cacheExpiration = 0
        }

        // If in developer mode cacheExpiration is set to 0 so each fetch will retrieve values from
        // the server.
        if (debugMode) {
            cacheExpiration = 0
        }

        // cacheExpirationSeconds is set to cacheExpiration here, indicating that any previously
        // fetched and cached config would be considered expired because it would have been fetched
        // more than cacheExpiration seconds ago. Thus the next fetch would go to the server unless
        // throttling is in progress. The default expiration duration is 43200 (12 hours).
        firebaseRemoteConfig.fetch(cacheExpiration)
            .addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    if (Firely.logLevel().debugLogEnabled()) {
                        Log.d(LOG_TAG, "Fetch Succeeded")
                    }
                    prefs.edit()
                        .putBoolean(SHARED_PREF_INITIAL_CHECK, true)
                        .apply()

                    activateFetched()
                } else {
                    if (Firely.logLevel().debugLogEnabled()) {
                        Log.d(LOG_TAG, "Fetch Failed")
                    }
                }
            }
    }

    fun activateFetched() {
        // Might not be already fetched
        if (Firely.logLevel().debugLogEnabled()) {
            Log.d(LOG_TAG, "Fetch Activated")
        }
        firebaseRemoteConfig.activate()
        updateAllTrackingProperties()
    }

    private fun updateAllTrackingProperties() {
        allPropsWithCurrentValue.clear()

        config.allValues().associate { value ->
            value.key to firebaseRemoteConfig.getString(value.key)
        }.also {
            allPropsWithCurrentValue.putAll(it)
        }
    }
}
