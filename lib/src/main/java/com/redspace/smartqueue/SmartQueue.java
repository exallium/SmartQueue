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
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * SmartQueue -- Threadsafe Event Queueing with Deferred Dependencies and whatnot.
 *
 * @param <E> Event type parameter.  Must be an Enum
 * @param <D> Data type parameter.
 */
public class SmartQueue<E extends Enum, D> {

    private final Queue<SmartQueueRecord<E, D>> delegate = new PriorityBlockingQueue<>();

    private final Set<E> seenEvents = new TreeSet<>();
    private final Map<E, Queue<SmartQueueRecord<E, D>>> deferedRecords = new HashMap<>();
    private final SmartQueueWorker<E, D> smartQueueWorker;

    private final Set<Class> dependencies = new TreeSet<>();

    private final WeakReference<SmartQueueLogger> weakLogger;

    private SmartQueue(SmartQueueProcessor<E, D> processor, SmartQueueLogger logger) {
        weakLogger = new WeakReference<>(logger);
        smartQueueWorker = new SmartQueueWorker<>(processor);
    }

    /**
     * Creates a SmartQueue instance.  Sets up and starts Worker thread.
     * @param processor "Where" events will go when they are dequeued, owned by caller.
     */
    public static <E extends Enum, D> SmartQueue<E, D> create(SmartQueueProcessor<E, D> processor) {
        return create(processor, EMPTY_LOGGER);
    }

    public static <E extends Enum, D> SmartQueue<E, D> create(SmartQueueProcessor<E, D> processor,
                                                              SmartQueueLogger logger) {
        SmartQueue<E, D> smartQueue = new SmartQueue<>(processor, logger);
        smartQueue.smartQueueWorker.setQueue(smartQueue);
        smartQueue.smartQueueWorker.start();
        smartQueue.getLogger().info("SmartQueue Created");
        return smartQueue;
    }

    /**
     * Add a class dependency.  Useful for plugin architectures.
     * @param klass The class that can be depended on.
     */
    public synchronized void addDependency(Class klass) {
        getLogger().debug(String.format("Adding Dependency: %s", klass.getSimpleName()));
        dependencies.add(klass);
    }

    /**
     * Remove a class dependency.  Useful for plugin architectures.
     * @param klass The class that can no longer be depended on.
     */
    public synchronized void removeDependency(Class klass) {
        getLogger().debug(String.format("Removing Dependency: %s", klass.getSimpleName()));
        dependencies.remove(klass);
    }

    SmartQueueLogger getLogger() {
        return weakLogger.get();
    }

    /**
     * Creates and passes back a new record builder.  User must call submit()
     * @param event Event to queue
     * @param data  Data to queue
     * @return A new builder object, which you can populate with extra optional data.
     */
    public RecordBuilder<E, D> createRecord(E event, D data) {
        getLogger().debug(String.format("Create RecordBuilder for Event %s and Data %s", event, data));
        return new RecordBuilder<>(event, data, this);
    }

    private void add(SmartQueueRecord<E, D> record) {
        getLogger().debug(String.format("add(%s)", record.toString()));
        synchronized (smartQueueWorker) {
            delegate.add(record);
            smartQueueWorker.notify();
        }
    }

    SmartQueueRecord<E, D> remove() {
        if (smartQueueWorker.getId() != Thread.currentThread().getId()) {
            throw new IllegalAccessError("Only the Worker Thread can dequeue objects");
        }

        final SmartQueueRecord<E, D> removed;
        synchronized (smartQueueWorker) {
            if (!delegate.isEmpty()) {
                removed = delegate.remove();
            } else {
                getLogger().debug("remove() -> null");
                return null;
            }
        }

        if (isRecordValid(removed) && !shouldDefer(removed)) {
            enqueueDeferred(removed);
            getLogger().debug(String.format("remove() -> %s", removed));
            return removed;
        }

        getLogger().debug("remove() -> null");
        return null;
    }

    void onWorkerDone() {
        getLogger().debug("onWorkerDone()");
        synchronized (smartQueueWorker) {
            if (delegate.isEmpty()) {
                try {
                    getLogger().verbose("Awaiting More Events");
                    smartQueueWorker.wait();
                }
                catch (InterruptedException e) {
                    getLogger().error("Thread was interrupted during wait", e);
                }
            }
        }
    }

    private void enqueueDeferred(SmartQueueRecord<E, D> record) {
        E event = record.getEvent();
        seenEvents.add(event);

        if (deferedRecords.containsKey(event)) {
            Queue<SmartQueueRecord<E, D>> queue = deferedRecords.get(event);
            getLogger().verbose(String.format("Enqueuing %d Deferred Events for %s", queue.size(), event.name()));
            delegate.addAll(queue);
            deferedRecords.remove(event);
        }
    }

    private boolean isRecordValid(SmartQueueRecord<E, D> record) {
        Class dependency = record.getDependsOn();
        E defer = record.getDeferUntil();
        if (dependency != null
                && !dependencies.contains(dependency)
                && (defer == null || deferedRecords.containsKey(defer))) {
            getLogger().info(String.format("Removing Invalid Record: %s", record.toString()));
            return false;
        }

        long creationDate = record.getCreatedAt();
        long lifespan = record.getLifespan();
        long now = System.currentTimeMillis();
        boolean isValid = !(lifespan != 0 && lifespan < now - creationDate);
        if (!isValid) {
            getLogger().info(String.format("Removing Invalid Record: %s", record.toString()));
        }
        return isValid;

    }

    private boolean shouldDefer(SmartQueueRecord<E, D> record) {
        E deferType = record.getDeferUntil();
        if (deferType != null
                && !seenEvents.contains(deferType)) {
            defer(record);
            return true;
        }
        return false;
    }

    private void defer(SmartQueueRecord<E, D> record) {
        E deferType = record.getDeferUntil();
        getLogger().verbose(String.format("Deferring record until event %s is seen.", deferType.name()));
        Queue<SmartQueueRecord<E, D>> deferQueue = deferedRecords.get(deferType);
        if (deferQueue == null) {
            deferQueue = new PriorityBlockingQueue<>();
            deferedRecords.put(deferType, deferQueue);
        }
        deferQueue.add(record);
    }

    /**
     * Builds a SmartQueueRecord and allows insertion into this Queue
     * @param <E> Event Enumeration Type
     * @param <D> Data type
     */
    public static class RecordBuilder<E extends Enum, D> {

        private final SmartQueueRecord.Builder<E, D> delegate;
        private final WeakReference<SmartQueue<E, D>> weakSmartQueue;

        private RecordBuilder(E event, D data, SmartQueue<E, D> smartQueue) {
            this.delegate = new SmartQueueRecord.Builder<>(event, data);
            this.weakSmartQueue = new WeakReference<>(smartQueue);
        }

        /**
         * Set lifespan for record
         * @param millis  Millis to live for
         * @return this builder.
         */
        public RecordBuilder<E, D> withLifespan(long millis) {
            delegate.withLifespan(millis);
            return this;
        }

        /**
         * Set priority for record
         * @param priority From SmartQueuePriority
         * @return this builder.
         */
        public RecordBuilder<E, D> withPriority(SmartQueuePriority priority) {
            delegate.withPriority(priority);
            return this;
        }

        /**
         * Set an event to require first
         * @param eventType The required event
         * @return this builder.
         */
        public RecordBuilder<E, D> deferUntil(E eventType) {
            delegate.deferUntil(eventType);
            return this;
        }

        /**
         * Set a dependency class that this relies on
         * @param klass The class to depend on
         * @return this builder
         */
        public RecordBuilder<E, D> dependsOn(Class klass) {
            delegate.dependsOn(klass);
            return this;
        }

        /**
         * Add the built record to the Queue
         */
        public void submit() {
            SmartQueue<E, D> smartQueue = weakSmartQueue.get();
            if (smartQueue != null) {
                smartQueue.add(delegate.build());
            }
        }
    }

    private static final SmartQueueLogger EMPTY_LOGGER = new SmartQueueLogger() {

        @Override
        public void critical(String message, Throwable t) {

        }

        @Override
        public void error(String message, Throwable t) {

        }

        @Override
        public void warn(String message, Throwable t) {

        }

        @Override
        public void info(String message, Throwable t) {

        }

        @Override
        public void debug(String message, Throwable t) {

        }

        @Override
        public void verbose(String message, Throwable t) {

        }
    };
}
