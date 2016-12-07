package org.pcj.internal;

import org.pcj.PCJ;
import org.pcj.internal.faulttolerance.FaultTolerancePolicy;
import org.pcj.internal.message.MessageSyncGo;
import org.pcj.internal.utils.BitMask;

import java.io.IOException;
import java.util.Set;

import static org.pcj.internal.InternalPCJ.getNetworker;
import static org.pcj.internal.InternalPCJ.getWorkerData;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 2/17/15
 * Time: 12:32 AM
 */
public class BarrierHandler {

    private final FaultTolerancePolicy policy;
    private Integer groupId;

    public BarrierHandler(FaultTolerancePolicy policy) {
        this.policy = policy;
    }

    public void setGroupUnderBarrier(int groupId) {
        this.groupId = groupId;
    }

    public void resetBarrier() {
        groupId = null;
    }

    public void finishBarrierIfInProgress(Set<Integer> failedThreads) throws IOException {
        if (groupId != null) {
            InternalGroup group = getWorkerData().internalGroupsById.get(groupId);
            if (group.getPhysicalMaster() != PCJ.getPhysicalNodeId()) {
                return;
            }
            final BitMask physicalSync = group.getPhysicalSync();
            synchronized (physicalSync) {
                if (physicalSync.isSet()) {
                    physicalSync.clear();
                    MessageSyncGo msg = new MessageSyncGo();
                    msg.setGroupId(groupId);
                    msg.setFailedThreads(failedThreads);
                    getNetworker().send(group, msg);
                }
            }
        }
    }

    public void markCompleteOnPhysicalNode(int groupId, int physicalId) throws IOException {     // mstodo move back to internal group!!
        InternalGroup group = getWorkerData().internalGroupsById.get(groupId);
        final BitMask physicalSync = group.getPhysicalSync();
//        System.out.println("will mark complete on physical node: " + physicalId + " physicalSync: " + physicalSync);
        synchronized (physicalSync) {
            if (group.physicalSync(physicalId)) {
                physicalSync.clear();

                MessageSyncGo msg = new MessageSyncGo();
                msg.setGroupId(groupId);

                getNetworker().send(group, msg);
            }
        }
    }
}
