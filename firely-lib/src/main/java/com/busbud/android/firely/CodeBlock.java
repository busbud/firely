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

import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class CodeBlock extends Operation {

    private List<String> mVariant;

    CodeBlock(String name, InternalFirely internalConfig) {
        super(name, internalConfig);
    }

    public CodeBlock withVariant(String... keys) {
        mVariant = Arrays.asList(keys);
        return this;
    }

    public void execute(IDefaultCodeBranch defaultCodeBranch, ICodeBranch... branches) {
        if (defaultCodeBranch == null) {
            return;
        }
        if (branches == null) {
            defaultCodeBranch.execute();
            return;
        }
        if (branches.length != (mVariant != null ? mVariant.size() : 0)) {
            if (Firely.logLevel().errorLogEnabled()) {
                Log.e(CodeBlock.class.getSimpleName(), "Variants does not match CodeBranches");
            }
            // But continue.
        }
        String phoneVariant = getInternalFirely().getString(getName());
        if (mVariant != null && mVariant.contains(phoneVariant)) {
            int index = mVariant.indexOf(phoneVariant);
            if (branches.length > index) {
                branches[index].execute();
            } else {
                defaultCodeBranch.execute();
            }
        } else {
            defaultCodeBranch.execute();
        }
    }
}
