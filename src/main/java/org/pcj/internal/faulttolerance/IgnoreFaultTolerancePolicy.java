package org.pcj.internal.faulttolerance;

import org.pcj.internal.InternalPCJ;
import org.pcj.internal.message.MessageNodeFailed;

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
    @Override
    public void handleNodeFailure(int failedNodeId) {
        List<Integer> failedNodes = new ArrayList<>();
        System.out.println("\n\n\n\nnode failed\n\n");
        Set<Integer> physicalNodes = InternalPCJ.getWorkerData().getPhysicalNodes().keySet();   // todo: is synchronization needed?
        for (Integer node : physicalNodes) {
            if (!node.equals(failedNodeId)) {
                propagateFailure(failedNodeId, node, failedNodes);
                // todo for now we don't do anything with failed nodes, we could already notify others about failure
            }
        }

        // mstodo: inform other nodes about failure
        // mstodo: remove node from all groups, maps etc
    }

    private void propagateFailure(int failedNodeId, Integer node, List<Integer> failedNodes) {
        try {
            InternalPCJ.getNetworker().sendToPhysicalNode(node, new MessageNodeFailed(failedNodeId));
        } catch (IOException e) {
            e.printStackTrace();
            failedNodes.add(failedNodeId);
        }
    }
}
