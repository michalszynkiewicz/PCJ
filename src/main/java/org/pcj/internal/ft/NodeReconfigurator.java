package org.pcj.internal.ft;

import org.pcj.PCJ;
import org.pcj.internal.futures.GroupBarrierState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.pcj.StreamUtils.ONE_MATCHES;
import static org.pcj.internal.ConfigurationLock.callUnderWriteLock;
import static org.pcj.internal.ConfigurationLock.runUnderReadLock;
import static org.pcj.internal.InternalPCJ.getNodeData;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 3/16/18
 */
public class NodeReconfigurator {

    public boolean handleNodeRemoved(int failedNodePhysicalId, List<SetChild> communicationUpdates) {
        System.out.println("[" + PCJ.getNodeId() + "] handling removal of " + failedNodePhysicalId);
        boolean result = callUnderWriteLock(() -> {
            List<Integer> failedThreadIds = PCJ.getNodeData().getPhysicalIdByThreadId()
                    .entrySet().stream()
                    .filter(e -> e.getValue().equals(failedNodePhysicalId))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            FailureRegister.get().addFailure(failedNodePhysicalId, failedThreadIds);
            boolean modificationsMade = getNodeData().getGlobalGroup().applyTreeUpdates(communicationUpdates);

            removeFromInternalStructures(failedNodePhysicalId);
            modificationsMade |= removeFromGroups(failedNodePhysicalId, failedThreadIds, communicationUpdates);

            return modificationsMade;
        });

        Optional<SetChild> parentUpdate = communicationUpdates.stream()
                .filter(update -> update.getChild() != null && update.getChild().equals(PCJ.getNodeId()))
                .findFirst();

        if (parentUpdate.isPresent()) {
            runUnderReadLock(() -> resendBarrierReachedMessages());
        }

        return result;
    }

    private void resendBarrierReachedMessages() {
        PCJ.getNodeData().getGroups()
                .stream()
                .flatMap(group -> group.getBarrierStateMap().values().stream())
                .forEach(GroupBarrierState::process);
    }

    private boolean removeFromGroups(int physicalId, List<Integer> failedThreadIds, Collection<SetChild> treeUpdates) {
        return PCJ.getNodeData().getGroups()
                .stream()
                .map(g -> g.removeNode(physicalId, failedThreadIds, treeUpdates))
                .reduce(false, ONE_MATCHES);
    }

    private void removeFromInternalStructures(int failedNodePhysicalId) {
        Map<Integer, Integer> physicalIdByThreadId = getNodeData().getPhysicalIdByThreadId();
        List<Map.Entry<Integer, Integer>> entriesWithFailedNodeId = physicalIdByThreadId.entrySet().stream()
                .filter(e -> e.getValue().equals(failedNodePhysicalId))
                .collect(Collectors.toList());
        entriesWithFailedNodeId.forEach(e -> physicalIdByThreadId.remove(e.getKey()));

        getNodeData().getSocketChannelByPhysicalId().remove(failedNodePhysicalId);

        if (PCJ.getNodeId() == 0) {
            getNodeData().getNode0Data().getFinishedBitmask().set(failedNodePhysicalId);
        }
    }

    private static final NodeReconfigurator instance = new NodeReconfigurator();

    public static NodeReconfigurator get() {
        return instance;
    }
}
