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

import android.content.Context
import kotlin.reflect.KClass

object Firely {

    private var logLevel = LogLevel.NONE
    private lateinit var internal: InternalFirely

    fun setup(context: Context) : Firely {
        // Find the auto-generated FirelyConfig
        val firelyConfig = try {
            Class.forName("com.busbud.android.firely.FirelyConfig").newInstance() as IFirelyConfig
        } catch (e: Exception) {
            throw IllegalArgumentException("Unable to read com.busbud.android.firely.FirelyConfig generated class")
        }

        // Start remote config
        try {
            internal = InternalFirely(context, firelyConfig)
        } catch (e: Exception) {
            throw  IllegalArgumentException("Unable to instantiate FirebaseRemoteConfig", e)
        }

        return this
    }

    fun setDebugMode(debugMode: Boolean): Firely {
        internal.setDebugMode(debugMode)
        return this
    }

    fun setLogLevel(logLevel: LogLevel): Firely {
        this.logLevel = logLevel
        return this
    }

    fun logLevel() = logLevel

    private fun checkSetupCalled() {
        if (!this::internal.isInitialized) {
            throw IllegalAccessError("Need to call setup() first.")
        }
    }

    fun codeBlock(name: IFirelyItem): CodeBlock {
        checkSetupCalled()
        if (name.getName().isEmpty()) {
            throw IllegalArgumentException("Empty variable")
        }
        return CodeBlock(name.getName(), internal)
    }

    fun orderedArrayBlock(name: IFirelyItem): OrderedArrayBlock {
        checkSetupCalled()
        if (name.getName().isEmpty()) {
            throw IllegalArgumentException("Empty variable")
        }
        return OrderedArrayBlock(name.getName(), internal)
    }

    fun doubleVariable(name: IFirelyItem): LiveVariable<Double> {
        return variable(name, Double::class)
    }

    fun booleanVariable(name: IFirelyItem): LiveVariable<Boolean> {
        return variable(name, Boolean::class)
    }

    fun integerVariable(name: IFirelyItem): LiveVariable<Int> {
        return variable(name, Int::class)
    }

    fun stringVariable(name: IFirelyItem): LiveVariable<String> {
        return variable(name, String::class)
    }

    fun longVariable(name: IFirelyItem): LiveVariable<Long> {
        return variable(name, Long::class)
    }

    private fun <T : Any> variable(name: IFirelyItem, clazz: KClass<T>): LiveVariable<T> {
        checkSetupCalled()
        if (name.getName().isEmpty()) {
            throw  IllegalArgumentException("Empty variable")
        }
        return LiveVariable(name.getName(), internal, clazz)
    }

    fun getValuesAsMap(): Map<String, String> {
        checkSetupCalled()
        return getFirelyInternalConfig().getCurrentKnownValues()
    }

    fun getFirelyInternalConfig(): InternalFirely {
        return internal
    }
}
