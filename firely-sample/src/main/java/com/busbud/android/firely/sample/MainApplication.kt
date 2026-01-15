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

package com.busbud.android.firely.sample

import android.app.Application
import com.busbud.android.firely.Firely
import com.google.firebase.FirebaseOptions

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        simpleSetup()
//        differentFirebaseProjectSetup()
    }

    private fun simpleSetup() {
        Firely.setup(this).setDebugMode(BuildConfig.DEBUG)
    }

    private fun differentFirebaseProjectSetup() {
        val firebaseOptions = FirebaseOptions.Builder()
            .setProjectId("[SET-YOUR-PROJECT-ID-HERE]")
            .setApplicationId("[SET-YOUR-APP-ID-HERE]")
            .setApiKey("[SET-YOUR-API-KEY-HERE]")
            .build()

        Firely.setup(this, secondaryFirebaseAppOptions = firebaseOptions)
            .setDebugMode(BuildConfig.DEBUG)
    }
}
