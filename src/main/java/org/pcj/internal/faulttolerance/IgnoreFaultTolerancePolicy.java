package org.pcj.internal.faulttolerance;

import org.pcj.internal.InternalPCJ;
import org.pcj.internal.LogUtils;
import org.pcj.internal.WorkerData;
import org.pcj.internal.message.MessageFinished;
import org.pcj.internal.message.MessageNodeFailed;
import org.pcj.internal.message.MessageNodeRemoved;

import java.io.IOException;
import java.util.*;

import static java.util.Arrays.asList;
import static org.pcj.internal.InternalPCJ.getBarrierHandler;
import static org.pcj.internal.InternalPCJ.getWorkerData;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/25/15
 * Time: 10:43 PM
 */
public class IgnoreFaultTolerancePolicy implements FaultTolerancePolicy {

    private Set<Integer> failedNodes = new HashSet<>();
    private Set<Integer> failedThreads = new HashSet<>();

    @Override
    public void handleNodeFailure(int failedNodeId) {
        if (failedNodes.contains(failedNodeId)) {
            return;
        }
        failedNodes.add(failedNodeId);
        System.out.println("[" + new Date() + "]Acquiring write lock for node removal: " + failedNodeId + "...");
        Lock.writeLock();
        System.out.println("[" + new Date() + "]DONE");
        failedThreads.addAll(getWorkerData().getVirtualNodes(failedNodeId));
        List<SetChild> updates;
        try {
            updates = InternalPCJ.getNode0Data().remove(failedNodeId);
            finishBarrierIfInProgress(failedNodeId);

            mockNodeFinish(getWorkerData());
        } finally {
            Lock.writeUnlock();
        }

        Set<Integer> physicalNodes = getWorkerData().getPhysicalNodes().keySet();   // todo: is synchronization needed?
        for (Integer node : physicalNodes) {
//            sending to the failed node will fail
            if (!node.equals(failedNodeId)) {
                propagateFailure(failedNodeId, node, updates);
            }
        }

    }

    @Override
    public void reportError(int nodeId) {
        System.err.println("reporting error for nodeId: " + nodeId);
        System.err.println(asList(Thread.currentThread().getStackTrace()));
        MessageNodeFailed message = new MessageNodeFailed(nodeId);
        try {
            InternalPCJ.getNetworker().sendToPhysicalNode(0, message);
        } catch (IOException e) {
            e.printStackTrace();

            System.err.println("Node 0 failed. Quitting JVM");
            System.exit(12);
        }
    }

    private void finishBarrierIfInProgress(int failedNodeId) {
        try {
            getBarrierHandler().finishBarrierIfInProgress(failedNodeId, failedThreads);
            System.out.println("barrier should be released");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mockNodeFinish(WorkerData data) {
        try {
            InternalPCJ.getNetworker().send(data.getInternalGlobalGroup(), new MessageFinished());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void propagateFailure(int failedNodeId, Integer node, List<SetChild > updates) {
        try {
//            LogUtils.setEnabled(true);
            MessageNodeRemoved message = new MessageNodeRemoved(failedNodeId);
            message.setCommunicationUpdates(updates);
            InternalPCJ.getNetworker().sendToPhysicalNode(node, message);
        } catch (IOException e) {
            System.err.println("Error trying to send message to node: " + failedNodeId);
            e.printStackTrace();
        }
    }

}
