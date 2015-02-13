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

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/25/15
 * Time: 10:43 PM
 */
public class IgnoreFaultTolerancePolicy implements FaultTolerancePolicy {
    // mstodo throw exceptoins in places where we're waiting for something to come
    @Override
    public void handleNodeFailure(int failedNodeId) {           // mstodo maybe lock should be at this level?
        List<Integer> failedNodes = new ArrayList<>();
        System.out.println("\n\n\n\nnode failed\n\n");

        PCJ.getWorkerData().removePhysicalNode(failedNodeId);

        WorkerData data = InternalPCJ.getWorkerData();
        sendMessageFinished(data);
        Set<Integer> physicalNodes = data.getPhysicalNodes().keySet();   // todo: is synchronization needed?
        int root = InternalPCJ.getWorkerData().getInternalGlobalGroup().getPhysicalMaster();
        for (Integer node : physicalNodes) {
//            sending to the failed node will fail && there's no point in sending to current node == master
            if (!node.equals(failedNodeId) && node != root) {
                propagateFailure(failedNodeId, node, failedNodes);  // mstodo handle things gathered in failedNodes
            }
        }
    }

    private void sendMessageFinished(WorkerData data) { // mstodo replace with invoking local method!!!
        try {
            System.out.println("WILL SEND MESSAGE FINISHED BECAUSE OF NODE FAILURE");
            InternalPCJ.getNetworker().send(data.getInternalGlobalGroup(), new MessageFinished());
        } catch (IOException e) {
            e.printStackTrace(); // mstodo
        }
    }

    private void propagateFailure(int failedNodeId, Integer node, List<Integer> failedNodes) {
        try {
            InternalPCJ.getNetworker().sendToPhysicalNode(node, new MessageNodeRemoved(failedNodeId));
        } catch (IOException e) {
            e.printStackTrace();
            failedNodes.add(failedNodeId);
        }
    }
}
