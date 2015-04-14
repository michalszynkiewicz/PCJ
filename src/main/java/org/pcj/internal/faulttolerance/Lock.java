package org.pcj.internal.faulttolerance;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/12/15
 * Time: 5:25 PM
 */
public class Lock {
    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public static void readLock() {
        readWriteLock.readLock().lock();
    }

    public static void readUnlock() {
        readWriteLock.readLock().unlock();
    }
    public static void writeLock() {
        readWriteLock.writeLock().lock();
    }

    public static void writeUnlock() {
        readWriteLock.writeLock().unlock();
    }
}
