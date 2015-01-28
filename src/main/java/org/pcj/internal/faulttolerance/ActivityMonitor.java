package org.pcj.internal.faulttolerance;

import org.pcj.internal.message.MessagePing;
import org.pcj.internal.utils.Configuration;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.currentTimeMillis;
import static org.pcj.internal.InternalPCJ.getNetworker;
import static org.pcj.internal.InternalPCJ.getWorkerData;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/16/15
 * Time: 9:57 PM
 */
public class ActivityMonitor implements Runnable {
    private final Thread monitoringThread = new Thread(this);

    private final MessagePing messagePing = new MessagePing();
    private final FaultTolerancePolicy faultTolerancePolicy;

    private Map<Integer, Long> responseTimes = new ConcurrentHashMap<>();

    private long startTime;

    public ActivityMonitor(FaultTolerancePolicy faultTolerancePolicy) {
        this.faultTolerancePolicy = faultTolerancePolicy;
    }

    public void start() {
        monitoringThread.setDaemon(true);
        startTime = currentTimeMillis();
        monitoringThread.start();
    }

    public void pong(int physicalNodeId) {
        responseTimes.put(physicalNodeId, currentTimeMillis());
    }

    @Override
    public void run() {
        while (true) {
            Set<Integer> physicalNodes = getWorkerData().getPhysicalNodes().keySet();
            for (Integer nodeId : physicalNodes) {
                if (isTimedOut(nodeId)
                        || !sendPing(nodeId)) {
                    handleNodeFailure(nodeId);
                }
            }
            if (!sleep()) break;
        }
    }

    private boolean sendPing(Integer nodeId) {
        try {
            getNetworker().sendToPhysicalNode(nodeId, messagePing);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("node ping failed for node: " + nodeId);
            return false;
        }
    }

    private boolean isTimedOut(Integer nodeId) {
        Long lastResponseTime = responseTimes.get(nodeId);
        if (lastResponseTime == null) {
            lastResponseTime = startTime;
        }
        boolean timedOut = lastResponseTime - currentTimeMillis() < Configuration.NODE_TIMEOUT * 1000l;
        if (timedOut) {
            System.err.println("Node timed out: " + nodeId);
        }
        return timedOut;
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

    private void handleNodeFailure(int nodeId) {
        faultTolerancePolicy.handleNodeFailure(nodeId);
    }
}
