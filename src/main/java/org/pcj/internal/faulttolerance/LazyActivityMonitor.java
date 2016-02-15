package org.pcj.internal.faulttolerance;

import org.pcj.internal.InternalGroup;
import org.pcj.internal.LogUtils;
import org.pcj.internal.message.MessagePing;
import org.pcj.internal.utils.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.currentTimeMillis;
import static org.pcj.internal.InternalPCJ.getNetworker;
import static org.pcj.internal.InternalPCJ.getWorkerData;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 9/7/15
 * Time: 4:25 PM
 */
public class LazyActivityMonitor implements Runnable {
    private Thread monitoringThread = new Thread(this, "LazyActivityMonitorThread");

    private MessagePing messagePing = new MessagePing();
    private Map<Integer, Long> lastPingTimes = new HashMap<>();
    private final FaultTolerancePolicy faultTolerancePolicy;


    public LazyActivityMonitor(FaultTolerancePolicy faultTolerancePolicy) {
        this.faultTolerancePolicy = faultTolerancePolicy;
        monitoringThread.setDaemon(true);
        monitoringThread.setPriority(Thread.MAX_PRIORITY);
    }

    public void start() {
        initChildren();
        monitoringThread.start();
    }

    public synchronized void initChildren() {
        InternalGroup group = getWorkerData().getInternalGlobalGroup();
        addChildToObserve(group.getPhysicalLeft());
        addChildToObserve(group.getPhysicalRight());
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

    public synchronized void pingFromChild(int childId) {           // mstodo call it from worker
        lastPingTimes.put(childId, currentTimeMillis());
    }

    private void pingParent() {
        Integer parentId = getWorkerData().getInternalGlobalGroup().getPhysicalParent();
        int physicalId = getWorkerData().getPhysicalId();
        try {
            if (parentId >= 0) {
//                LogUtils.log(physicalId, "Will ping parent: " + parentId);
                getNetworker().sendToPhysicalNode(parentId, messagePing);
            }
        } catch (IOException e) {
//            LogUtils.log(physicalId, "Failed to blarbla b-cause of error" + e.getMessage());
            reportError(parentId);
        }
    }

    private synchronized void checkChildrenForFailure() {
        Lock.readLock();
        try {
            InternalGroup globalGroup = getWorkerData().getInternalGlobalGroup();
            List<Integer> observedNodes = new ArrayList<>();
            Integer left = globalGroup.getPhysicalLeft();
            Integer right = globalGroup.getPhysicalRight();
            if (left != null) observedNodes.add(left);
            if (right != null) observedNodes.add(right);
            observedNodes.forEach(this::checkForChildFailure);
        } finally {
            Lock.readUnlock();
        }
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
//            LogUtils.log(getWorkerData().getPhysicalId(), "Child node timed out: " + nodeId);
            reportError(nodeId);
        }
    }

    private void reportError(int nodeId) {
        faultTolerancePolicy.reportError(nodeId, false);
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

    public FaultTolerancePolicy getFaultTolerancePolicy() {
        return faultTolerancePolicy;
    }
}
