/*
 * Copyright (c) 2011-2017, PCJ Library
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.ft;

import org.pcj.internal.Configuration;
import org.pcj.internal.ConfigurationLock;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.message.MessagePing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

/**
 *
 * Checks if the child nodes are active.
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 2/15/18
 */
public class ActivityMonitor implements Runnable {
    private ActivityMonitor() {
        monitoringThread.setDaemon(true);
        monitoringThread.setPriority(Thread.MAX_PRIORITY);
    }

    private Thread monitoringThread = new Thread(this, "LazyActivityMonitorThread");

    private MessagePing messagePing = new MessagePing();
    private Map<Integer, Long> lastPingTimes = new HashMap<>();


    public void start() {
        initChildren();
        monitoringThread.setDaemon(true);
        monitoringThread.start();
    }

    public synchronized void initChildren() {
        getGlobalGroup()
                .getChildrenNodes()
                .forEach(this::addChildToObserve);
    }

    public synchronized void addChildToObserve(Integer child) {
        if (child != null) {
            lastPingTimes.put(child, currentTimeMillis());
        }
    }

    @Override
    public void run() {
        while (sleep()) {
            pingParent();
            checkChildrenForFailure();
        }
    }

    public synchronized void pingFromChild(int childId) {
        lastPingTimes.put(childId, currentTimeMillis());
    }

    private void pingParent() {
        Integer parentId = getGlobalGroup().getParentNode();
        if (parentId >= 0) {
            Emitter.get().send(parentId, messagePing);
        }
    }

    private InternalCommonGroup getGlobalGroup() {
        return InternalPCJ.getNodeData().getGlobalGroup();
    }

    private synchronized void checkChildrenForFailure() {
        List<Integer> observedNodes = new ArrayList<>();
        ConfigurationLock.runUnderReadLock(() -> {
                    InternalCommonGroup globalGroup = getGlobalGroup();
                    observedNodes.addAll(globalGroup.getChildrenNodes());
                }
        );
        observedNodes.forEach(this::checkForChildFailure);
    }

    private void checkForChildFailure(int nodeId) {
        Long lastPingTime = lastPingTimes.get(nodeId);
        if (lastPingTime == null) {
            lastPingTime = currentTimeMillis();
            lastPingTimes.put(nodeId, lastPingTime); // new observed node
        }
        long sinceLastPing = currentTimeMillis() - lastPingTime;
        if (sinceLastPing > Configuration.NODE_TIMEOUT * 1000L) {
            System.out.println("node: " + nodeId + " timed out after: " + sinceLastPing);
            reportError(nodeId);
        }
    }

    private void reportError(int nodeId) {
        FailurePropagator.get().notifyAboutFailure(nodeId, null);
    }

    private boolean sleep() {
        try {
            Thread.sleep(Configuration.NODE_PING_INTERVAL);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static final ActivityMonitor instance = new ActivityMonitor();

    public static ActivityMonitor get() {
        return instance;
    }
}
