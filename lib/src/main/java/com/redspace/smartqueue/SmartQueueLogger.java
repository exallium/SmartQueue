/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Alex Hart
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.redspace.smartqueue;

public abstract class SmartQueueLogger {

    public abstract void critical(String message, Throwable t);
    public abstract void error(String message, Throwable t);
    public abstract void warn(String message, Throwable t);
    public abstract void info(String message, Throwable t);
    public abstract void debug(String message, Throwable t);
    public abstract void verbose(String message, Throwable t);

    public final void critical(String message) { critical(message, null); }
    public final void error(String message) { error(message, null); }
    public final void warn(String message) { warn(message, null); }
    public final void info(String message) { info(message, null); }
    public final void debug(String message) { debug(message, null); }
    public final void verbose(String message) { verbose(message, null); }
}
