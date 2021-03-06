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

package com.redspace.smartqueue.impl;

import com.redspace.smartqueue.SmartQueueLogger;

public class SystemOutLogger extends SmartQueueLogger {

    private void printStackTrace(Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
    }

    private void printLog(String message, Throwable t) {
        System.out.println(message);
        printStackTrace(t);
    }

    @Override
    public void critical(String message, Throwable t) {
        printLog(message, t);
    }

    @Override
    public void error(String message, Throwable t) {
        printLog(message, t);
    }

    @Override
    public void warn(String message, Throwable t) {
        printLog(message, t);
    }

    @Override
    public void info(String message, Throwable t) {
        printLog(message, t);
    }

    @Override
    public void debug(String message, Throwable t) {
        printLog(message, t);
    }

    @Override
    public void verbose(String message, Throwable t) {
        printLog(message, t);
    }
}
