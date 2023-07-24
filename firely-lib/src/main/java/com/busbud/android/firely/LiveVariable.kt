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

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import kotlin.reflect.KClass
import kotlin.reflect.cast

private const val TAG = "LiveVariable"

class LiveVariable<T : Any>(
    name: String,
    internalConfig: InternalFirely,
    private val clazz: KClass<T>
) : Operation(name, internalConfig) {

    fun get(): T = when (clazz) {
        Boolean::class -> clazz.cast(internalFirely.getBoolean(name))
        String::class -> clazz.cast(internalFirely.getString(name))
        Double::class -> clazz.cast(internalFirely.getDouble(name))
        Long::class -> clazz.cast(internalFirely.getLong(name))
        Integer::class -> {
            // Not supported in Firebase so let's be crazy
            clazz.cast(Integer.valueOf(internalFirely.getString(name)))
        }

        else -> throw ClassCastException("Unsupported")
    }

    fun observeRealTime(
        lifecycleOwner: LifecycleOwner,
        onError: (() -> Unit)? = null
    ): LiveVariableDisposable {
        val listenerRegistration =
            internalFirely.addConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    if (configUpdate.updatedKeys.contains(name)) {
                        internalFirely.activateFetched()
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.d(TAG, "Error observing remote configs", error)
                    onError?.invoke()
                }

            })
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                listenerRegistration.remove()
                super.onDestroy(owner)
            }
        })

        return object : LiveVariableDisposable {
            override fun dispose() {
                listenerRegistration.remove()
            }
        }
    }
}
