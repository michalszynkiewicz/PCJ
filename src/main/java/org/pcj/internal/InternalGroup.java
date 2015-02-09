/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import org.pcj.internal.message.*;
import org.pcj.internal.utils.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal (with common ClassLoader) representation of Group.
 * It contains common data for groups.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalGroup {

    final private Map<Integer, Integer> nodes; // groupId, globalId
    final private MessageSyncWait syncMessage;
    final private int groupId;
    final private String groupName;
    final private ConcurrentMap<Integer, BitMask> joinBitmaskMap;
    final private AtomicInteger nodeNum = new AtomicInteger(0);
    /**
     * list of local node group ids
     */
    final private ArrayList<Integer> localIds;
    /**
     * list of remote computers ids in this group (for
     * broadcast)
     */
    final private List<Integer> physicalIds;
    /**
     * sync
     */
    final private BitMask localSync;
    final private BitMask localSyncMask;
    final private BitMask physicalSync;
    final private WaitObject syncObject;
    /**
     * Physical Parent, Left, Right
     */
    final private CommunicationTree physicalCommunication;

    //private final InternalGroup g;
    protected InternalGroup(InternalGroup g) {
        this.groupId = g.groupId;
        this.groupName = g.groupName;
        this.joinBitmaskMap = g.joinBitmaskMap;

        this.nodes = g.nodes;
        this.syncMessage = g.syncMessage;

        this.localIds = g.localIds;
        this.physicalIds = g.physicalIds;

        this.localSync = g.localSync;
        this.localSyncMask = g.localSyncMask;

        this.physicalSync = g.physicalSync;

        this.syncObject = g.syncObject;

        this.physicalCommunication = g.physicalCommunication;
    }

    protected InternalGroup(int groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.joinBitmaskMap = new ConcurrentHashMap<>();

        nodes = new HashMap<>();
        syncMessage = new MessageSyncWait();
        syncMessage.setGroupId(groupId);

        localIds = new ArrayList<>();
        physicalIds = new ArrayList<>();

        localSync = new BitMask();
        localSyncMask = new BitMask();
        physicalSync = new BitMask();

        syncObject = new WaitObject();

        physicalCommunication = new CommunicationTree();
    }

    /**
     * Used while joining. joinBitmaskMap stores information
     * about nodes that received information about new node
     * (current) and send bonjour message.
     *
     * @param groupNodeId
     * @return
     */
    BitMask getJoinBitmask(int groupNodeId) {
        BitMask newMask = new BitMask(groupNodeId + 1);
        BitMask mask = joinBitmaskMap.putIfAbsent(groupNodeId, newMask);
        if (mask == null) {
            mask = newMask;
            synchronized (mask) {
                mask.set(groupNodeId);
            }
        }
        return mask;
    }

    /**
     * @return the nodeNum
     */
    int nextNodeNum() {
        return nodeNum.getAndIncrement();
    }

    synchronized int addPhysicalId(int id) {
        int index = physicalIds.indexOf(id);
        if (index < 0) {
            physicalIds.add(id);
            // FIXME: to wszystko psuje ---v-v-v---
            // sortowanie powinno być wykonywane w jakiś mądrzejszy sposób
            // ...
            // dodatkowo bez sortowania kolejność fizycznych węzłów na liście
            // jest różna, a co za tym idzie, broadcast może się zapętlić...
            // ...
            // kolejność powinna być w jakiś sposób wspólna dla wszystkich
            // nawet w trakcie tworzenia grupy
            // ...
            // może przez wysyłanie razem z numerem groupId, wartości index?
            //Collections.sort(physicalIds);
            index = physicalIds.size() - 1;
            physicalSync.insert(index, 0);
        }
        return index;
    }

    /**
     * adds info about new node in group, or nothing if
     * groupNodeId exists in group
     *
     * @param groupNodeId groupNodeId of adding node
     * @param globalNodeId globalNodeId of adding node
     * @param remotePhysicalNodeId physicalId of adding node
     */
    synchronized void add(int groupNodeId, int globalNodeId, int remotePhysicalNodeId) {
        if (nodes.containsKey(groupNodeId) == false) {
            nodes.put(groupNodeId, globalNodeId);
//            addPhysicalId(remotePhysicalNodeId);

            int size = Math.max(localSync.getSize(), groupNodeId + 1);
            localSync.setSize(size);
            localSyncMask.setSize(size);
            if (remotePhysicalNodeId == InternalPCJ.getWorkerData().physicalId) {
                localIds.add(groupNodeId);
                localSyncMask.set(groupNodeId);
            }
        }
    }

    synchronized int[] getPhysicalIds() {
        int[] ids = new int[physicalIds.size()];
        int i = 0;
        for (int id : physicalIds) {
            ids[i++] = id;
        }
        return ids;
    }

    boolean physicalSync(int physicalId) {
        int position = physicalIds.indexOf(physicalId);
        physicalSync.set(position);
        return physicalSync.isSet();
    }

    Integer getPhysicalMaster() {
//        if (physicalIds.isEmpty() == false) {
//            return physicalIds.getFutureObject(0);
//        }
//        return -1;
        return physicalCommunication.getRoot();
    }

    void setPhysicalMaster(int physicalMaster) {
        this.physicalCommunication.setRoot(physicalMaster);
    }

    Integer getPhysicalParent() {
//        int index = (physicalIndex - 1) / 2;
//        if (0 <= index && index < physicalIds.size()) {
//            return physicalIds.getFutureObject(index);
//        }
//        return -1;
        return physicalCommunication.getParent();
    }

    void setPhysicalParent(int physicalParent) {
        this.physicalCommunication.setParent(physicalParent);
    }

    Integer getPhysicalLeft() {
        List<Integer> children = physicalCommunication.getChildren();
        if (children.size() < 1) {
            return null;
        }
        return children.get(0);
    }

    void setPhysicalLeft(int physicalLeft) {
        List<Integer> children = physicalCommunication.getChildren();
        if (children.size() < 1) {
            children.add(physicalLeft);
        } else {
            children.set(0, physicalLeft);
        }
    }

    Integer getPhysicalRight() {
        List<Integer> children = physicalCommunication.getChildren();
        if (children.size() < 2) {
            return null;
        }
        return children.get(1);
    }

    void setPhysicalRight(int physicalRight) {
        List<Integer> children = physicalCommunication.getChildren();
        if (children.size() < 2) {
            children.add(physicalRight);
        } else {
            children.set(1, physicalRight);
        }
    }

    int getGroupId() {
        return groupId;
    }

    /**
     * @return the syncMessage
     */
    WaitObject getSyncObject() {
        return syncObject;
    }

    BitMask getPhysicalSync() {
        return physicalSync;
    }

    /**
     * @return the localIds
     */
    List<Integer> getLocalIds() {
        return localIds;
    }

    /**
     * Gets global node id from group node id
     *
     * @param nodeId group node id
     * @return global node id or -1 if group doesn't have
     * specified group node id
     */
    int getNode(int nodeId) {
        if (nodes.containsKey(nodeId)) {
            return nodes.get(nodeId);
        }
        return -1;
    }

    protected int myId() {
        throw new UnsupportedOperationException("This method have to be overriden!");
    }

    protected void barrier() {
        throw new UnsupportedOperationException("This method have to be overriden!");
    }

    protected String getGroupName() {
        return groupName;
    }

    protected int threadCount() {
        return nodes.size();
    }

    protected void barrier(int myNodeId) {
        syncObject.lock();
        try {
            localSync.set(myNodeId);
            if (localSync.isSet(localSyncMask)) {
                InternalPCJ.getNetworker().send(getPhysicalMaster(), syncMessage);
                localSync.clear();
            }
            syncObject.await();
        } catch (InterruptedException | IOException ex) {
            throw new RuntimeException(ex);
        }
        syncObject.unlock();
    }

    /**
     * Synchronize current node and node with specified group
     * nodeId
     *
     * @param nodeId group node id
     */
    protected void barrier(int myNodeId, int nodeId) {
        // FIXME: trzeba sprawdzić jak to będzie działać w pętli.
//        if (true) {
//            sync(myNodeId, new int[]{nodeId});
//        }

        /* change current group nodeId to global nodeId */
        nodeId = nodes.get(nodeId);
        myNodeId = nodes.get(myNodeId);

        try {
            MessageNodeSync msg = new MessageNodeSync();
            msg.setSenderGlobalNodeId(myNodeId);
            msg.setReceiverGlobalNodeId(nodeId);

            InternalPCJ.getNetworker().send(nodeId, msg);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        final Map<PcjThreadPair, PcjThreadPair> syncNode = InternalPCJ.getWorkerData().syncNodePair;
        PcjThreadPair pair = new PcjThreadPair(myNodeId, nodeId);
        synchronized (syncNode) {
            while (syncNode.containsKey(pair) == false) {
                try {
                    syncNode.wait();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
            pair = syncNode.remove(pair);
            if (pair.get() > 1) {
                pair.decrease();
                syncNode.put(pair, pair);
            }
        }
    }

    protected <T> InternalFutureObject<T> getFutureObject(InternalFutureObject<T> futureObject, int myNodeId, int nodeId, String variable, int... indexes) {
        nodeId = nodes.get(nodeId);

        Message msg;
        if (indexes.length == 0) {
            msg = new MessageValueAsyncGetRequest();
            ((MessageValueAsyncGetRequest) msg).setSenderGlobalNodeId(myNodeId);
            ((MessageValueAsyncGetRequest) msg).setReceiverGlobalNodeId(nodeId);
            ((MessageValueAsyncGetRequest) msg).setVariableName(variable);
        } else {
            msg = new MessageValueAsyncGetRequestIndexes();
            ((MessageValueAsyncGetRequestIndexes) msg).setSenderGlobalNodeId(myNodeId);
            ((MessageValueAsyncGetRequestIndexes) msg).setReceiverGlobalNodeId(nodeId);
            ((MessageValueAsyncGetRequestIndexes) msg).setIndexes(indexes);
            ((MessageValueAsyncGetRequestIndexes) msg).setVariableName(variable);
        }

        InternalPCJ.getWorkerData().attachmentMap.put(msg.getMessageId(), futureObject);
        try {
            InternalPCJ.getNetworker().send(nodeId, msg);
            return futureObject;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void put(int nodeId, String variable, Object newValue, int... indexes) {
        nodeId = nodes.get(nodeId);

        Message msg;
        if (indexes.length == 0) {
            msg = new MessageValuePut();
            ((MessageValuePut) msg).setReceiverGlobalNodeId(nodeId);
            ((MessageValuePut) msg).setVariableName(variable);
            ((MessageValuePut) msg).setVariableValue(CloneObject.serialize(newValue));
        } else {
            msg = new MessageValuePutIndexes();
            ((MessageValuePutIndexes) msg).setReceiverGlobalNodeId(nodeId);
            ((MessageValuePutIndexes) msg).setVariableName(variable);
            ((MessageValuePutIndexes) msg).setIndexes(indexes);
            ((MessageValuePutIndexes) msg).setVariableValue(CloneObject.serialize(newValue));
        }

        try {
            InternalPCJ.getNetworker().send(nodeId, msg);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void broadcast(String variable, Object newValue) {
        MessageValueBroadcast msg = new MessageValueBroadcast();
        msg.setGroupId(groupId);
        msg.setVariableName(variable);
        msg.setVariableValue(CloneObject.serialize(newValue));

        try {
            InternalPCJ.getNetworker().send(this, msg);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void log(int myNodeId, String text) {
        MessageLog msg = new MessageLog();
        msg.setGroupId(groupId);
        msg.setGroupNodeId(myNodeId);
        msg.setLogText(text);

        try {
            InternalPCJ.getNetworker().send(InternalPCJ.getNode0Socket(), msg);
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public void removePhysicalNode(int physicalNodeId, Set<Integer> virtualNodes) {
        physicalIds.remove(physicalNodeId);

        List<Integer> groupIdsToRemove = new ArrayList<>();
        for (Map.Entry<Integer, Integer> nodeEntry : nodes.entrySet()) {
            if (virtualNodes.contains(nodeEntry.getValue())) {
                groupIdsToRemove.add(nodeEntry.getKey());
            }
        }

        for (Integer groupId : groupIdsToRemove) {
            nodes.remove(groupId);
        }
    }
}
