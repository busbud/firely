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

import java.util.HashMap;

public class OrderedArrayBlock extends Operation {

    private static final String SEPERATOR = ",";

    private HashMap<String, ICodeBranch> mSteps = new HashMap<>();
    private String mSeparator = SEPERATOR;

    OrderedArrayBlock(String name, InternalFirely internalFirely) {
        super(name, internalFirely);
    }

    public OrderedArrayBlock initSeparator(String separator) {
        mSeparator = separator;
        return this;
    }

    public OrderedArrayBlock addStep(String key, ICodeBranch branch) {
        mSteps.put(key, branch);
        return this;
    }

    public void execute() {
        if (mSteps == null || mSteps.size() == 0) {
            return;
        }
        String arrayVariant = getInternalFirely().getString(getName());
        String[] variants = arrayVariant.split(mSeparator);
        for (String variant : variants) {
            String cleanVariant = variant.trim();
            ICodeBranch toExec = mSteps.get(cleanVariant);
            if (toExec == null) {
                if (Firely.logLevel().errorLogEnabled()) {
                    Log.e(OrderedArrayBlock.class.getSimpleName(), "Unknown values in " + getName());
                }
            } else {
                toExec.execute();
            }
        }
    }
}
