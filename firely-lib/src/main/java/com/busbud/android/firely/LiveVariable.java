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

public class LiveVariable<T> extends Operation {
    private Class<T> mClass;

    LiveVariable(String name, InternalFirely internalConfig, Class<T> clazz) {
        super(name, internalConfig);
        mClass = clazz;
    }

    public T get() {
        if (mClass.isAssignableFrom(Boolean.class)) {
            return mClass.cast(getInternalFirely().getBoolean(getName()));
        } else if (mClass.isAssignableFrom(String.class)) {
            return mClass.cast(getInternalFirely().getString(getName()));
        } else if (mClass.isAssignableFrom(Double.class)) {
            return mClass.cast(getInternalFirely().getDouble(getName()));
        } else if (mClass.isAssignableFrom(Long.class)) {
            return mClass.cast(getInternalFirely().getLong(getName()));
        } else if (mClass.isAssignableFrom(Integer.class)) {
            // Not supported in Firebase so let's be crazy
            return mClass.cast(Integer.valueOf(getInternalFirely().getString(getName())));
        }
        throw new ClassCastException("Unsupported");
    }
}
