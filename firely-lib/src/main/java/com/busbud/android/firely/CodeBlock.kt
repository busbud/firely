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

class CodeBlock(name: String, internalFirely: InternalFirely) : Operation(name, internalFirely) {

    private var variant = listOf<String>()

    fun withVariant(vararg keys: String): CodeBlock {
        variant = keys.toList()
        return this
    }

    fun execute(defaultCodeBranch: IDefaultCodeBranch, vararg branches: ICodeBranch) {

        if (branches.isEmpty()) {
            defaultCodeBranch.execute()
            return
        }

        if (branches.size != variant.size) {
            if (Firely.logLevel().errorLogEnabled()) {
                Log.e(CodeBlock::class.simpleName, "Variants does not match CodeBranches")
            }
            // But continue.
        }

        val phoneVariant: String = internalFirely.getString(name)
        if (variant.contains(phoneVariant)) {
            val index = variant.indexOf(phoneVariant)
            if (branches.size > index) {
                branches[index].execute()
            } else {
                defaultCodeBranch.execute()
            }
        } else {
            defaultCodeBranch.execute()
        }
    }
}
