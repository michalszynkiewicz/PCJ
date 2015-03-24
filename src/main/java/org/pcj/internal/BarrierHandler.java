package org.pcj.internal;

import org.pcj.internal.message.MessageSyncGo;
import org.pcj.internal.utils.BitMask;

import java.io.IOException;

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

    public void finishBarrierIfInFinished() throws IOException {
        System.out.print("[barrier] marking complete... ");
        if (groupId != null) {
            System.out.print("in progress for groupId: " + groupId);
            InternalGroup group = getWorkerData().internalGroupsById.get(groupId);
            final BitMask physicalSync = group.getPhysicalSync();
            synchronized (physicalSync) {
                if (physicalSync.isSet()) {
                    physicalSync.clear();

                    MessageSyncGo msg = new MessageSyncGo();
                    msg.setGroupId(groupId);
                    getNetworker().send(group, msg);
                }
            }
            System.out.println("DONE");
        } else {
            System.out.println("UNNECESSARY");
        }
    }

    public void markCompleteOnPhysicalNode(int groupId, int physicalId) throws IOException {     // mstodo move back to internal group!!
        InternalGroup group = getWorkerData().internalGroupsById.get(groupId);
        final BitMask physicalSync = group.getPhysicalSync();
        synchronized (physicalSync) {
            if (group.physicalSync(physicalId)) {
                System.out.println("BARRIER FINISHED");
                physicalSync.clear();

                MessageSyncGo msg = new MessageSyncGo();
                msg.setGroupId(groupId);
                getNetworker().send(group, msg);
            } else {
                System.out.println("BARRIER NOT FINISHED - bitmask: " +  physicalSync);
            }
        }
    }
}
