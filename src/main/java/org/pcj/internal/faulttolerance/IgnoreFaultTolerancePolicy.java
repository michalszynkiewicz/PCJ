package org.pcj.internal.faulttolerance;

import org.pcj.PCJ;
import org.pcj.internal.InternalGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.WorkerData;
import org.pcj.internal.message.MessageFinishCompleted;
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
        if (!finishIfLastNodeFailed(data)) {
            Set<Integer> physicalNodes = data.getPhysicalNodes().keySet();   // todo: is synchronization needed?
            for (Integer node : physicalNodes) {
                if (!node.equals(failedNodeId)) {
                    propagateFailure(failedNodeId, node, failedNodes);
                    // todo for now we don't do anything with failed nodes, we could already notify others about failure
                }
            }
        }
    }

    private boolean finishIfLastNodeFailed(WorkerData data) {
        if (data.getPhysicalNodesCount() == 0) {
            InternalGroup globalGroup = data.getInternalGlobalGroup();

            MessageFinishCompleted reply = new MessageFinishCompleted();
            try {
                InternalPCJ.getNetworker().send(globalGroup, reply);
            } catch (IOException e) {
                e.printStackTrace(); // mstodo
            }
            return true;
        }
        return false;
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
