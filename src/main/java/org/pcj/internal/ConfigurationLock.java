/*
 * Copyright (c) 2011-2017, PCJ Library
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 10/4/17
 */
public class ConfigurationLock {
    private static final Set<Thread> readLockThreads = Collections.synchronizedSet(new HashSet<>());
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    public static void runUnderReadLock(Runnable stuff) {
        callUnderReadLock(() -> {
            stuff.run();
            return null;
        });
    }
    // mstodo verify that returned value is needed
    public static <V> V callUnderReadLock(Supplier<V> stuff) {
        readLock();
        try {
            return stuff.get();
        } finally {
            readUnlock();
        }
    }

    public static void runUnderWriteLock(Runnable stuff) {
        callUnderWriteLock(() -> {
            stuff.run();
            return null;
        });
    }

    public static <V> V callUnderWriteLock(Supplier<V> stuff) {
        writeLock();
        try {
            return stuff.get();
        } finally {
            writeUnlock();
        }
    }

    private static void readLock() {
        lock.readLock().lock();
        readLockThreads.add(Thread.currentThread());
    }

    private static void readUnlock() {
        if (readLockThreads.remove(Thread.currentThread())) {
            lock.readLock().unlock();
        }
    }

    private static void writeLock() {
        lock.writeLock().lock();
    }

    private static void writeUnlock() {
        lock.writeLock().unlock();
    }

    public static void releaseCurrentThreadLocks() {
        if (lock.isWriteLockedByCurrentThread()) {
            writeUnlock();
        }
        Thread currentThread = Thread.currentThread();
        if (readLockThreads.contains(currentThread)) {
            readUnlock();
        }
    }

    public static boolean isUnderWriteLock() {
        return lock.isWriteLockedByCurrentThread();
    }

    public static boolean isUnderReadLock() {
        return readLockThreads.contains(Thread.currentThread());
    }
}
