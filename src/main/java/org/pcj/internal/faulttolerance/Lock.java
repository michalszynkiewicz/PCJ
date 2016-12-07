package org.pcj.internal.faulttolerance;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/12/15
 * Time: 5:25 PM
 */
public class Lock {
    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

//    private static ThreadLocal<Integer> localLockCount;
//    private static ThreadLocal<Integer> localWriteLockCount = new ThreadLocal<>();
//    private static AtomicInteger lockCount = new AtomicInteger(0);
//    private static AtomicInteger writeLockCount = new AtomicInteger(0);

    private static final Set<Thread> readLockThreads = Collections.synchronizedSet(new HashSet<>());

    public static void readLock() {
//        System.out.print("will read lock...\t");
        readWriteLock.readLock().lock();
        readLockThreads.add(Thread.currentThread());
//        int localCount = localWriteLockCount.get() == null ? 1 : localWriteLockCount.get() + 1;
//        localWriteLockCount.set(localCount);
//        System.out.println("DONE. read lock count:" + lockCount.incrementAndGet() +
//                " this thread read lock count: " + localCount);
//
//        Thread.dumpStack();
    }

    public static boolean isWriteLocked() {
        return readWriteLock.isWriteLocked();
    }

    public static void readUnlock() {
        Thread th = Thread.currentThread();
        if (readLockThreads.remove(th)) {
            readWriteLock.readLock().unlock();
        }
//        int localCount = localWriteLockCount.get() - 1;
//        localWriteLockCount.set(localCount);
//        System.out.println("unlocked: " + lockCount.decrementAndGet() +
//                " this thread read lock count: " + localCount);
    }

    public static void writeLock() {
//        System.out.println("############################will write lock from thread: " + Thread.currentThread().toString());
//        System.out.print("Will write-lock read lock count: : " + lockCount.get() +
//                " this thread read lock count: " + localWriteLockCount.get());
//        System.out.println("\twrite lock count: " + writeLockCount.get());
        readWriteLock.writeLock().lock();
//        writeLockCount.incrementAndGet();
//        System.out.println("##################################################");
//        Thread.dumpStack();
//        System.out.println("##################################################");
//        System.out.println("in the lock");

    }

    public static void writeUnlock() {
//        Integer localWriteLocks = localWriteLockCount.get() - 1;
//        localWriteLockCount.set(localWriteLocks);
//        System.out.println("Unlock: " + localWriteLocks);
//        System.out.println("write lock count: " + writeLockCount.decrementAndGet());
        readWriteLock.writeLock().unlock();
//        System.out.println("****************************************************");
//        System.out.println("UNLOCKED FROM");
//        Thread.dumpStack();
//        System.out.println("****************************************************");                       /*mstodo remove logging*/
    }

    public static void printLockState() {
        System.out.println(stateDescription());
    }

    public static String stateDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Write locked: " + readWriteLock.isWriteLocked()).append("\n");
        boolean writeLockable = readWriteLock.writeLock().tryLock();
        description.append("Write lockable: " + writeLockable + "\n");
        if (writeLockable) {
            readWriteLock.writeLock().unlock();
        }
        return description.toString();
    }

    public static void releaseCurrentThreadLocks() {
        if (readWriteLock.isWriteLockedByCurrentThread()) {
            writeUnlock();
        }
        Thread currentThread = Thread.currentThread();
        if (readLockThreads.contains(currentThread)) {
            readUnlock();
        }
    }
}
