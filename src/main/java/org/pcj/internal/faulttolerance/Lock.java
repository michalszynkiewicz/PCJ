package org.pcj.internal.faulttolerance;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/12/15
 * Time: 5:25 PM
 */
public class Lock {
    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

//    static ThreadLocal<Integer> localLockCount;
//    static ThreadLocal<Integer> localWriteLockCount = new ThreadLocal<>();
//    static AtomicInteger lockCount = new AtomicInteger(0);
//    static AtomicInteger writeLockCount = new AtomicInteger(0);

    public static void readLock() {
//        System.out.print("will read lock...\t");
        readWriteLock.readLock().lock();
//        int localCount = localWriteLockCount.get() == null ? 1 : localWriteLockCount.get() + 1;
//        localWriteLockCount.set(localCount);
//        System.out.println("DONE. read lock count:" + lockCount.incrementAndGet() +
//                " this thread read lock count: " + localCount);
//
//        Thread.dumpStack();
    }

    public static void readUnlock() {
        readWriteLock.readLock().unlock();
//        int localCount = localWriteLockCount.get() - 1;
//        localWriteLockCount.set(localCount);
//        System.out.println("unlocked: " + lockCount.decrementAndGet() +
//                " this thread read lock count: " + localCount);
    }

    public static void writeLock() {
//        Thread.dumpStack();
//        System.out.print("Will write-lock read lock count: : " + lockCount.get() +
//                " this thread read lock count: " + localWriteLockCount.get());
//        System.out.println("\twrite lock count: " + writeLockCount.get());
        readWriteLock.writeLock().lock();
//        System.out.println("in the lock");

    }

    public static void writeUnlock() {
//        Integer localWriteLocks = localWriteLockCount.get() - 1;
//        localWriteLockCount.set(localWriteLocks);
//        System.out.println("Unlock: " + localWriteLocks);
//        System.out.println("write lock count: " + writeLockCount.decrementAndGet());
        readWriteLock.writeLock().unlock();
    }
}
