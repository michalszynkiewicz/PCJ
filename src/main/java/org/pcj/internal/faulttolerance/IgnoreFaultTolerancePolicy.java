package org.pcj.internal.faulttolerance;

import org.pcj.PCJ;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.Node0Data;
import org.pcj.internal.WorkerData;
import org.pcj.internal.message.MessageFinished;
import org.pcj.internal.message.MessageNodeRemoved;

import java.io.IOException;
import java.util.*;

import static org.pcj.internal.InternalPCJ.*;

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
        System.out.println("[" + new Date() + "]Acquiring write lock...");
        Lock.writeLock();
        System.out.println("[" + new Date() + "]DONE");
        failedThreads.addAll(getWorkerData().getVirtualNodes(failedNodeId));
        Node0Data.CommunicationReplacement replacement;
        try {
            replacement = InternalPCJ.getNode0Data().remove(failedNodeId);
            finishBarrierIfInProgress(failedNodeId);

            getWorkerData().removePhysicalNode(failedNodeId);

            PCJ.getFutureHandler().nodeFailed(failedNodeId);

            mockNodeFinish(getWorkerData());
            getWaitForHandler().nodeFailed(failedNodeId);
        } finally {
            Lock.writeUnlock();
        }

        Set<Integer> physicalNodes = getWorkerData().getPhysicalNodes().keySet();   // todo: is synchronization needed?
        int root = 0;
        List<Integer> failedNodes = new ArrayList<>();

        for (Integer node : physicalNodes) {
//            sending to the failed node will fail && there's no point in sending to current node == master
            if (!node.equals(failedNodeId) && node != root) {
                propagateFailure(failedNodeId, node, failedNodes, replacement);  // mstodo handle things gathered in failedNodes
            }
        }
    }

    private void finishBarrierIfInProgress(int failedNodeId) {
        try {
            getBarrierHandler().finishBarrierIfInProgress(failedNodeId, failedThreads);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mockNodeFinish(WorkerData data) { // mstodo replace with invoking local method!!!
        try {
            InternalPCJ.getNetworker().send(data.getInternalGlobalGroup(), new MessageFinished());
        } catch (IOException e) {
            e.printStackTrace(); // mstodo
        }
    }

    private void propagateFailure(int failedNodeId, Integer node, List<Integer> failedNodes, Node0Data.CommunicationReplacement replacement) {
        try {
            MessageNodeRemoved message = new MessageNodeRemoved(failedNodeId);
            if (replacement.doesReplace()) {
                message.setNewCommunicationNode(replacement.parent);
                message.setNewCommunicationLeft(replacement.newLeftChild);
                message.setNewCommunicationRight(replacement.newRightChild);
            }
            InternalPCJ.getNetworker().sendToPhysicalNode(node, message);
        } catch (IOException e) {
            e.printStackTrace();
            failedNodes.add(failedNodeId);
        }
    }

}
