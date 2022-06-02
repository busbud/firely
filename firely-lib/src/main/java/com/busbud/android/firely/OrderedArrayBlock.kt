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

private const val SEPARATOR = ","

class OrderedArrayBlock(
    name: String,
    internalConfig: InternalFirely
) : Operation(name, internalConfig) {

    private val steps: HashMap<String, ICodeBranch> = HashMap()
    private var separator: String = SEPARATOR


    fun initSeparator(separator: String): OrderedArrayBlock {
        this.separator = separator
        return this
    }

    fun addStep(key: String, branch: ICodeBranch): OrderedArrayBlock {
        steps[key] = branch
        return this
    }

    fun execute() {
        if (steps.size == 0) {
            return
        }

        val arrayVariant: String = internalFirely.getString(name)
        val variants: List<String> = arrayVariant.split(separator)
        variants.forEach {
            val cleanVariant = it.trim()
            val toExec: ICodeBranch? = steps[cleanVariant]
            if (toExec == null) {
                if (Firely.logLevel().errorLogEnabled()) {
                    Log.e(OrderedArrayBlock::class.simpleName, "Unknown values in $name")
                }
            } else {
                toExec.execute()
            }
        }
    }
}
