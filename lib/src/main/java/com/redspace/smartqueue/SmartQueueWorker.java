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

import java.lang.ref.WeakReference;

final class SmartQueueWorker<E extends Enum, D> extends Thread {

    private WeakReference<SmartQueue<E, D>> weakSmartQueue = new WeakReference<>(null);
    private WeakReference<SmartQueueProcessor<E, D>> weakProcessor = new WeakReference<>(null);

    public SmartQueueWorker(SmartQueueProcessor<E, D> processor) {
        this.weakProcessor = new WeakReference<>(processor);
    }

    void setQueue(SmartQueue<E, D> queue) {
        weakSmartQueue = new WeakReference<>(queue);
    }

    @Override
    public void run() {
        while (true) {
            SmartQueue<E, D> smartQueue = weakSmartQueue.get();
            SmartQueueProcessor<E, D> smartQueueProcessor = weakProcessor.get();
            if (smartQueue == null || smartQueueProcessor == null) {
                return;
            }

            SmartQueueRecord<E, D> record;
            while ((record = smartQueue.remove()) != null) {
                smartQueue.getLogger().debug(String.format("Worker Processing %s", record.toString()));
                smartQueueProcessor.process(record.getEvent(), record.getData());
            }

            smartQueue.onWorkerDone();
        }
    }
}
