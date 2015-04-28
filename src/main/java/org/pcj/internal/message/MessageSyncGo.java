/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.network.MessageInputStream;
import org.pcj.internal.network.MessageOutputStream;

import java.util.HashSet;
import java.util.Set;

/**
 * @see MessageTypes#SYNC_GO
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageSyncGo extends BroadcastedMessage {

    private int groupId;
    private Set<Integer> failedThreads;

    public MessageSyncGo() {
        super(MessageTypes.SYNC_GO);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(groupId);
        bbos.writeIntArray(getFailedThreadsAsArray());
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        groupId = bbis.readInt();
        failedThreads = new HashSet<>();
        for (int thread : bbis.readIntArray()) {
            failedThreads.add(thread);
        }
    }
    
    @Override
    public String paramsToString() {
        return "";
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    private int[] getFailedThreadsAsArray() {
        if (failedThreads == null) {
            return new int[0];
        }
        int[] arr = new int[failedThreads.size()];
        int i = 0;
        for (Integer thread : failedThreads) {
            arr[i++] = thread;
        }
        return arr;
    }

    public Set<Integer> getFailedThreads() {
        return failedThreads;
    }

    public void setFailedThreads(Set<Integer> failedThreads) {
        this.failedThreads = failedThreads;
    }
}
