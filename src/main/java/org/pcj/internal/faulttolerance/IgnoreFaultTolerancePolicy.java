package org.pcj.internal.faulttolerance;

import org.pcj.PCJ;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.WorkerData;
import org.pcj.internal.message.MessageFinished;
import org.pcj.internal.message.MessageNodeRemoved;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.pcj.internal.InternalPCJ.*;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/25/15
 * Time: 10:43 PM
 */
public class IgnoreFaultTolerancePolicy implements FaultTolerancePolicy {
    @Override
    public void handleNodeFailure(int failedNodeId) {           // mstodo maybe lock should be at this level?
        List<Integer> failedNodes = new ArrayList<>();
        System.out.println("NODE FAILED!!! will handle node failure!");
        Lock.writeLock();
        try {
            System.out.println("\n\n\n\nnode failed\n\n");

            getWorkerData().removePhysicalNode(failedNodeId);

            PCJ.getFutureHandler().nodeFailed(failedNodeId);
            finishBarrierIfInProgress();

            mockNodeFinish(getWorkerData());
            getWaitForHandler().nodeFailed(failedNodeId);
        } finally {
            Lock.writeUnlock();
        }
        Set<Integer> physicalNodes = getWorkerData().getPhysicalNodes().keySet();   // todo: is synchronization needed?
        int root = getWorkerData().getInternalGlobalGroup().getPhysicalMaster();
        for (Integer node : physicalNodes) {
//            sending to the failed node will fail && there's no point in sending to current node == master
            if (!node.equals(failedNodeId) && node != root) {
                propagateFailure(failedNodeId, node, failedNodes);  // mstodo handle things gathered in failedNodes
            }
        }
    }

    private void finishBarrierIfInProgress() {
        try {
            getBarrierHandler().finishBarrierIfInFinished();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mockNodeFinish(WorkerData data) { // mstodo replace with invoking local method!!!
        try {
            System.out.println("WILL SEND MESSAGE FINISHED BECAUSE OF NODE FAILURE");
            InternalPCJ.getNetworker().send(data.getInternalGlobalGroup(), new MessageFinished());
        } catch (IOException e) {
            e.printStackTrace(); // mstodo
        }
    }

    private void propagateFailure(int failedNodeId, Integer node, List<Integer> failedNodes) {
        try {
            System.out.println("will send node " +failedNodeId+ "  removed to node: " + node);
            InternalPCJ.getNetworker().sendToPhysicalNode(node, new MessageNodeRemoved(failedNodeId));
        } catch (IOException e) {
            e.printStackTrace();
            failedNodes.add(failedNodeId);
        }
    }
}
