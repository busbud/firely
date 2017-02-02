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

package com.busbud.android.firely;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.smaspe.iterables.FuncIter;

import java.util.HashMap;
import java.util.Map;

class InternalFirely {

    private static final String LOG_TAG = Firely.class.getSimpleName();

    private final FirebaseRemoteConfig mFirebaseRemoteConfig;
    private final IFirelyConfig mConfig;
    private Map<String, String> mAllPropsWithCurrentValue;
    private SharedPreferences mPrefs;
    private boolean mDebugMode;

    InternalFirely(Context context, IFirelyConfig firelyConfig) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mConfig = firelyConfig;

        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
                // Do Nothing
            }

            @Override
            public void onActivityStarted(Activity activity) {
                // Do nothing
            }

            @Override
            public void onActivityResumed(Activity activity) {
                // Fetch the app while active
                fetch();
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (mDebugMode) {
                    activateFetched();
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                // Do Nothing
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
                // Do Nothing
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (!mDebugMode && activity.isTaskRoot() && activity.isFinishing()) {
                    activateFetched();
                }
            }
        });

        // Set defaults from IFirelyConfig to FirebaseRemoteConfig
        final HashMap<String, Object> defaults = new HashMap<>();
        FuncIter.iter(firelyConfig.allValues())
                .each(new FuncIter.Exec<IFirelyItem>() {
                    @Override
                    public void call(IFirelyItem value) {
                        defaults.put(value.getName(), value.getDefault());
                    }
                });
        mFirebaseRemoteConfig.setDefaults(defaults);

        // Init tracking properties from FirebaseRemoteConfig for tracking
        updateAllTrackingProperties();
    }

    String getString(String name) {
        return mFirebaseRemoteConfig.getString(name);
    }

    Boolean getBoolean(String name) {
        return mFirebaseRemoteConfig.getBoolean(name);
    }

    Double getDouble(String name) {
        return mFirebaseRemoteConfig.getDouble(name);
    }

    Long getLong(String name) {
        return mFirebaseRemoteConfig.getLong(name);
    }

    Map<String, String> getCurrentKnownValues() {
        return mAllPropsWithCurrentValue;
    }

    void setDebugMode(boolean debugMode) {
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(debugMode)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        mDebugMode = debugMode;
    }

    private void fetch() {

        long cacheExpiration = 7200; // 1 hour in seconds - 5 requests per hour limitation

        // Hack to be sure to force fetch the configuration at first launch
        if (!mPrefs.getBoolean(SharedPreferencesKey.RemoteConfig.INITIAL_CHECK, false)) {
            cacheExpiration = 0;
        }

        // If in developer mode cacheExpiration is set to 0 so each fetch will retrieve values from
        // the server.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        // cacheExpirationSeconds is set to cacheExpiration here, indicating that any previously
        // fetched and cached config would be considered expired because it would have been fetched
        // more than cacheExpiration seconds ago. Thus the next fetch would go to the server unless
        // throttling is in progress. The default expiration duration is 43200 (12 hours).
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> result) {
                        if (result.isSuccessful()) {
                            if (Firely.logLevel().debugLogEnabled()) {
                                Log.d(LOG_TAG, "Fetch Succeeded");
                            }
                            mPrefs.edit().putBoolean(SharedPreferencesKey.RemoteConfig.INITIAL_CHECK, true).apply();
                        } else {
                            if (Firely.logLevel().debugLogEnabled()) {
                                Log.d(LOG_TAG, "Fetch Failed");
                            }
                        }
                    }
                });
    }

    private void activateFetched() {
        // Might not be already fetched
        if (Firely.logLevel().debugLogEnabled()) {
            Log.d(LOG_TAG, "Fetch Activated");
        }
        mFirebaseRemoteConfig.activateFetched();
        updateAllTrackingProperties();
    }

    private void updateAllTrackingProperties() {
        final HashMap<String, String> args = new HashMap<>();
        FuncIter.iter(mConfig.allValues())
                .each(new FuncIter.Exec<IFirelyItem>() {
                    @Override
                    public void call(IFirelyItem value) {
                        args.put(value.getName(), mFirebaseRemoteConfig.getString(value.getName()));
                    }
                });
        mAllPropsWithCurrentValue = args;
    }
}
