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

import android.content.Context;
import android.text.TextUtils;

import java.util.Map;

public class Firely {

    private static Firely sInstance;
    private static LogLevel sLogLevel = LogLevel.NONE;

    public static Firely setup(Context context) {
        if (sInstance == null) {
            sInstance = new Firely(context);
        }
        return sInstance;
    }

    private InternalFirely mInternal;

    private Firely(Context context) {

        // Find the auto-generated FirelyConfig
        final IFirelyConfig firelyConfig;
        try {
            Class firelyConfigClass = Class.forName("com.busbud.android.firely.FirelyConfig");
            firelyConfig = (IFirelyConfig) firelyConfigClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to read com.busbud.android.firely.FirelyConfig generated class");
        }

        // Start remote config
        try {
            mInternal = new InternalFirely(context, firelyConfig);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to instantiate FirebaseRemoteConfig", e);
        }
    }

    public Firely setDebugMode(boolean debugMode) {
        mInternal.setDebugMode(debugMode);
        return this;
    }

    public Firely setLogLevel(LogLevel level) {
        sLogLevel = level;
        return this;
    }

    public static LogLevel logLevel() {
        return sLogLevel;
    }

    private static void checkSetupCalled() {
        if (sInstance == null) {
            throw new IllegalAccessError("Need to call setup() first.");
        }
    }

    public static CodeBlock codeBlock(IFirelyItem name) {
        checkSetupCalled();
        if (name == null || TextUtils.isEmpty(name.getName())) {
            throw new IllegalArgumentException("Empty variable");
        }
        return new CodeBlock(name.getName(), sInstance.getFirelyInternalConfig());
    }

    public static OrderedArrayBlock orderedArrayBlock(IFirelyItem name) {
        checkSetupCalled();
        if (name == null || TextUtils.isEmpty(name.getName())) {
            throw new IllegalArgumentException("Empty variable");
        }
        return new OrderedArrayBlock(name.getName(), sInstance.getFirelyInternalConfig());
    }

    public static LiveVariable<Double> doubleVariable(IFirelyItem name) {
        return variable(name, Double.class);
    }

    public static LiveVariable<Boolean> booleanVariable(IFirelyItem name) {
        return variable(name, Boolean.class);
    }

    public static LiveVariable<Integer> integerVariable(IFirelyItem name) {
        return variable(name, Integer.class);
    }

    public static LiveVariable<String> stringVariable(IFirelyItem name) {
        return variable(name, String.class);
    }

    public static LiveVariable<Long> longVariable(IFirelyItem name) {
        return variable(name, Long.class);
    }

    private static <T> LiveVariable<T> variable(IFirelyItem name, Class<T> clazz) {
        checkSetupCalled();
        if (name == null || TextUtils.isEmpty(name.getName())) {
            throw new IllegalArgumentException("Empty variable");
        }
        return new LiveVariable<T>(name.getName(), sInstance.getFirelyInternalConfig(), clazz);
    }

    public static Map<String, String> getValuesAsMap() {
        checkSetupCalled();
        return sInstance.getFirelyInternalConfig().getCurrentKnownValues();
    }

    private InternalFirely getFirelyInternalConfig() {
        return mInternal;
    }
}
