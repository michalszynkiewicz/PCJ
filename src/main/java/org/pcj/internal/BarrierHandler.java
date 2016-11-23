package org.pcj.internal;

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

    private Integer groupId;

    public void setGroupUnderBarrier(int groupId) {
        this.groupId = groupId;
    }

    public void resetBarrier() {
        this.groupId = null;
    }

    public void finishBarrierIfInProgress(int failedNodeId, Set<Integer> failedThreads) throws IOException {
//        LogUtils.setEnabled(true);
//        LogUtils.log("[barrier] marking complete... ");
        if (groupId != null) {
//            LogUtils.log("in progress for groupId: " + groupId);
            InternalGroup group = getWorkerData().internalGroupsById.get(groupId);
            final BitMask physicalSync = group.getPhysicalSync();
            int position = group.indexOf(failedNodeId);
            if (position < 0) {
                return;
            }
            synchronized (physicalSync) {
                System.out.println("physical sync after node removal: " + physicalSync);
//                LogUtils.log(getWorkerData().physicalId, );
                if (!physicalSync.isSet(position)) {
                    physicalSync.set(position);
                }
                if (physicalSync.isSet()) {
//                    LogUtils.log(getWorkerData().physicalId, "Barrier to finish");
                    physicalSync.clear();
//                    LogUtils.log("will send sync go from failure handler");
                    MessageSyncGo msg = new MessageSyncGo();
                    msg.setGroupId(groupId);
                    msg.setFailedThreads(failedThreads);
                    getNetworker().send(group, msg);
//                    LogUtils.log("will send sync message to finish barrier: " + msg);
//                    LogUtils.log(getWorkerData().physicalId, "sent sync go");
//                } else {
//                    LogUtils.log(getWorkerData().physicalId, "Barrier not finished yet, bitmask: " + physicalSync.toString());
                }
//                }
            }
        } else {
            // LogUtils.log("UNNECESSARY");
        }
    }

    public void markCompleteOnPhysicalNode(int groupId, int physicalId) throws IOException {     // mstodo move back to internal group!!
        InternalGroup group = getWorkerData().internalGroupsById.get(groupId);
        final BitMask physicalSync = group.getPhysicalSync();
        synchronized (physicalSync) {
            if (group.physicalSync(physicalId)) {
                physicalSync.clear();

                MessageSyncGo msg = new MessageSyncGo();
                msg.setGroupId(groupId);

                getNetworker().send(group, msg);
//                LogUtils.log("will send sync go from markComplete");// mstodo rem ove
                // LogUtils.log("BARRIER FINISHED - bitmask: " + physicalSync);
            } else {
                // LogUtils.log("BARRIER NOT FINISHED - bitmask: " + physicalSync);
            }
        }
    }
}
