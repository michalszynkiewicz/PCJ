package org.pcj.internal.ft;

import org.pcj.PCJ;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.pcj.internal.ConfigurationLock.runUnderWriteLock;
import static org.pcj.internal.InternalPCJ.getNodeData;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 3/16/18
 */
public class NodeReconfigurator {

    public void handleNodeRemoved(int failedNodePhysicalId, List<SetChild> communicationUpdates) {
        runUnderWriteLock(() -> {
            List<Integer> failedThreadIds = PCJ.getNodeData().getPhysicalIdByThreadId()
                    .entrySet().stream()
                    .filter(e -> e.getValue().equals(failedNodePhysicalId))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            System.out.println("failed threads: " + failedThreadIds); // mstodo remove
            FailureRegister.get().addFailure(failedNodePhysicalId, failedThreadIds);
            getNodeData().getGlobalGroup().applyTreeUpdates(communicationUpdates);

            removeFromInternalStructures(failedNodePhysicalId);
            removeFromGroups(failedNodePhysicalId, failedThreadIds);
        });
    }

    private void removeFromGroups(int physicalId, List<Integer> failedThreadIds) {
        PCJ.getNodeData().getGroups()
                .forEach(g -> g.removeNode(physicalId, failedThreadIds));
    }

    private void removeFromInternalStructures(int failedNodePhysicalId) {
        Map<Integer, Integer> physicalIdByThreadId = getNodeData().getPhysicalIdByThreadId();
        List<Map.Entry<Integer, Integer>> entriesWithFailedNodeId = physicalIdByThreadId.entrySet().stream()
                .filter(e -> e.getValue().equals(failedNodePhysicalId))
                .collect(Collectors.toList());
        entriesWithFailedNodeId.forEach(e -> physicalIdByThreadId.remove(e.getKey()));

        getNodeData().getSocketChannelByPhysicalId().remove(failedNodePhysicalId);

        if (PCJ.getNodeId() == 0) {
            System.out.println("setting " + failedNodePhysicalId + " in finished bitmask");                            // mstodo remove
            getNodeData().getNode0Data().getFinishedBitmask().set(failedNodePhysicalId);
            System.out.println("bitmask after: " + getNodeData().getNode0Data().getFinishedBitmask()); // mstodo remove
        }
    }

    private static final NodeReconfigurator instance = new NodeReconfigurator();

    public static NodeReconfigurator get() {
        return instance;
    }
}
