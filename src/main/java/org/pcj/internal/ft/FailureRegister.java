/*
 * Copyright (c) 2011-2017, PCJ Library
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.ft;

import org.pcj.internal.ConfigurationLock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.synchronizedList;

/**
 *
 * Keeps track of node failures.
 *
 * This class assumes that registering a failure does not happen in parallel
 * with user code run.
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/22/17
 */
public class FailureRegister {
    private final Set<Integer> failedThreadIds = new HashSet<>();
    private final Set<Integer> failedPhysicalNodeIds = new HashSet<>();

    private final Set<Integer> newFailedNodes = new HashSet<>();
    private final Set<Integer> newFailedThreads = new HashSet<>();

    private final List<Thread> threadsWaitingForFailure = synchronizedList(new ArrayList<>());

    private FailureRegister(){}

    public void addFailure(Integer physicalNodeId, Collection<Integer> threadIds) {
        assert ConfigurationLock.isUnderWriteLock();
        newFailedNodes.add(physicalNodeId);
        newFailedThreads.addAll(threadIds);

        threadsWaitingForFailure.forEach(Thread::interrupt);
    }

    public void registerFailureWatchingThread(Thread thread) {
        assert ConfigurationLock.isUnderReadLock();
        threadsWaitingForFailure.add(thread);
        if (!newFailedNodes.isEmpty()) {
            threadsWaitingForFailure.forEach(Thread::interrupt);
            markFailuresRegistered();
        }
    }

    public void unregisterFailureWatchingThread(Thread thread) {
        assert ConfigurationLock.isUnderReadLock();
        threadsWaitingForFailure.remove(thread);
    }

    public Set<Integer> getFailedThreadIds() {
        assert ConfigurationLock.isUnderReadLock();
        markFailuresRegistered();
        return failedThreadIds;
    }

    public Set<Integer> getFailedPhysicalNodeIds() {
        assert ConfigurationLock.isUnderReadLock();
        markFailuresRegistered();
        return failedPhysicalNodeIds;
    }

    public void failOnNewFailure() {
        assert ConfigurationLock.isUnderReadLock();
        if (!newFailedNodes.isEmpty()) {
            List<Integer> failures = new ArrayList<>(newFailedNodes);
            List<Integer> failedThreads = new ArrayList<>();
            markFailuresRegistered();
            throw new NodeFailedException(failures, failedThreads);
        }
    }


//    private synchronized boolean failIfThreadFailed(Integer threadId) {
//        if (failedThreadIds.contains(threadId) || newFailedThreads.contains(threadId)) {
//
//        }
//    }

    private synchronized void markFailuresRegistered() {
        failedThreadIds.addAll(newFailedThreads);
        failedPhysicalNodeIds.addAll(newFailedNodes);
        newFailedThreads.clear();
        newFailedNodes.clear();
    }

    public static final FailureRegister instance = new FailureRegister();

    public static FailureRegister get() {
        return instance;
    }

    public synchronized void failIfThreadFailed(int threadId) {
        if (isThreadFailed(threadId)) {
             throwNFE();
         }
    }

    public boolean isThreadFailed(int threadId) {
        return failedThreadIds.contains(threadId) || newFailedThreads.contains(threadId);
    }

    private void throwNFE() {
        throw createNFE();
    }

    public NodeFailedException createNFE() {
        return new NodeFailedException(failedPhysicalNodeIds, failedThreadIds);
    }
}
