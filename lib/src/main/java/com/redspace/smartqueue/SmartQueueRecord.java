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

final class SmartQueueRecord<E extends Enum, D> implements Comparable<SmartQueueRecord<E, D>> {

    private final long createdAt;
    private final long lifespan;
    private final SmartQueuePriority priority;
    private final E deferUntil;
    private final Class dependsOn;
    private final D data;
    private final E event;

    @Override
    public int compareTo(SmartQueueRecord<E, D> o) {
        int priorityA = priority.ordinal();
        int priorityB = o.getPriority().ordinal();

        if (priorityA > priorityB) {
            return -1;
        } else if (priorityB < priorityA) {
            return 1;
        } return 0;
    }

    static class Builder<E extends Enum, D> {
        private long lifespan = 0;
        private SmartQueuePriority priority = SmartQueuePriority.NORMAL;
        private E deferUntil = null;
        private Class dependsOn = null;
        private final D data;
        private final E event;

        public Builder(E event, D data) {
            this.event = event;
            this.data = data;
        }

        public Builder<E, D> withLifespan(long millis) {
            this.lifespan = Math.max(millis, 0);
            return this;
        }

        public Builder<E, D> withPriority(SmartQueuePriority priority) {
            this.priority = priority == null ? SmartQueuePriority.NORMAL : priority;
            return this;
        }

        public Builder<E, D> deferUntil(E eventType) {
            if (eventType == event) {
                throw new IllegalStateException("Cannot Defer to own event type");
            }
            this.deferUntil = eventType;

            return this;
        }

        public Builder<E, D> dependsOn(Class klass) {
            this.dependsOn = klass;
            return this;
        }

        public SmartQueueRecord<E, D> build() {
            return new SmartQueueRecord<>(this);
        }

    }

    private SmartQueueRecord(Builder<E, D> builder) {
        this.createdAt = System.currentTimeMillis();
        this.lifespan = builder.lifespan;
        this.deferUntil = builder.deferUntil;
        this.dependsOn = builder.dependsOn;
        this.priority = builder.priority;
        this.data = builder.data;
        this.event = builder.event;
    }

    long getCreatedAt() {
        return createdAt;
    }

    long getLifespan() {
        return lifespan;
    }

    SmartQueuePriority getPriority() {
        return priority;
    }

    E getDeferUntil() {
        return deferUntil;
    }

    Class getDependsOn() {
        return dependsOn;
    }

    D getData() {
        return data;
    }

    E getEvent() {
        return event;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Record::");
        builder.append("|ev").append(event.name());
        builder.append("|data\"").append(data.toString()).append("\"");
        builder.append("|pri").append(priority.name());
        if (deferUntil != null) {
            builder.append("|dep").append(deferUntil.name());
        }
        return builder.toString();
    }
}
