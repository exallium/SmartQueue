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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Random;

@RunWith(JUnit4.class)
public class TestSmartQueue {

    private enum EventType {
        A, B, C, D
    }

    private SmartQueue<EventType, String> smartQueue;
    private int count;

    private final SmartQueueProcessor<EventType, String> eventQueueProcessor = new SmartQueueProcessor<EventType, String>() {
        @Override
        public void process(EventType event, String data) {
            System.out.println(data);
            count++;
        }
    };

    @Before
    public void setUp() {
        smartQueue = new SmartQueue<>(eventQueueProcessor);
        smartQueue.setDebugEnabled(true);
    }

    /**
     * Creates 1000 records and submits them.  Waits 3 seconds, and then makes sure
     * all of them were processed.
     */
    @Test
    public void testAdd() {
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            EventType e = EventType.values()[Math.abs(random.nextInt()) % 4];
            EventType d = EventType.values()[Math.abs(random.nextInt()) % 4];
            smartQueue.createRecord(e, "" + i)
                    .deferUntil(e == d ? null : d)
                    .withPriority(SmartQueuePriority.values()[Math.abs(random.nextInt()) % 4])
                    .submit();
        }

        doWait(3000);
        Assert.assertTrue(count == 1000);
    }

    private void doWait(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
