/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import org.pcj.internal.faulttolerance.FaultTolerancePolicy;
import org.pcj.internal.faulttolerance.Lock;
import org.pcj.internal.faulttolerance.NodeFailedException;
import org.pcj.internal.faulttolerance.SetChild;
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
    private BitMask localSync;
    private BitMask localSyncMask;
    private BitMask physicalSync;
    final private WaitObject syncObject;
    /**
     * Physical Parent, Left, Right
     */
    final private CommunicationTree physicalCommunication;

    private final Set<Integer> removedNodes = new HashSet<>();

    //private final InternalGroup g;
    protected InternalGroup(InternalGroup g) {
        System.out.println(this);
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
        System.out.println(this);
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
     * @param groupNodeId          groupNodeId of adding node
     * @param globalNodeId         globalNodeId of adding node
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

    int indexOf(int physicalId) {    // mstodo where used?
        return physicalIds.indexOf(physicalId);
    }

    boolean physicalSync(int physicalId) {
//        System.out.println("physical sync before: " + physicalSync);
        int position = physicalIds.indexOf(physicalId);
        physicalSync.set(position);
//        System.out.println("physical sync after: " + physicalSync);
        return physicalSync.isSet();
    }

    public Integer getPhysicalMaster() {
//        if (physicalIds.isEmpty() == false) {
//            return physicalIds.getFutureObject(0);
//        }
//        return -1;
        return physicalCommunication.getRoot();
    }

    void setPhysicalMaster(int physicalMaster) {
        this.physicalCommunication.setRoot(physicalMaster);
    }

    public Integer getPhysicalParent() {
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

    public Integer getPhysicalLeft() {
        List<Integer> children = physicalCommunication.getChildren();
        if (children.size() < 1) {
            return null;
        }
        return children.get(0);
    }

    void setPhysicalLeft(Integer physicalLeft) {
        List<Integer> children = physicalCommunication.getChildren();
        if (physicalLeft != null) {
            if (children.size() < 1) {
                children.add(physicalLeft);
            } else {
                children.set(0, physicalLeft);
            }
        } else {
            children.clear(); // we have a fully balanced tree - removal of left child means there's no right child eiter
        }
    }

    public Integer getPhysicalRight() {
        List<Integer> children = physicalCommunication.getChildren();
        if (children.size() < 2) {
            return null;
        }
        return children.get(1);
    }

    void setPhysicalRight(Integer physicalRight) {
        List<Integer> children = physicalCommunication.getChildren();
        if (physicalRight != null) {
            if (children.size() < 2) {
                children.add(physicalRight);
            } else {
                children.set(1, physicalRight);
            }
        } else {
            physicalCommunication.setChildren(children.subList(0, 1));
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
     * @return global node id or -1 if group doesn't have specified group node
     * id
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
        LogUtils.log(myNodeId, "barrier] will do"); // todo virtual node in log
        Lock.readLock();
        boolean unlocked = false;
        LogUtils.log(myNodeId, "barrier] lock"); // todo virtual node in log

        try {
            syncObject.lock();
            InternalPCJ.getBarrierHandler().setGroupUnderBarrier(groupId);
            try {
                localSync.set(myNodeId);
//                System.out.println("barrier] set localSync");
                if (localSync.isSet(localSyncMask)) {
//                    System.out.println("barrier] localSync is set");
                    Lock.readUnlock();
                    unlocked = true;
                    try {
//                         System.out.println("barrier] sending syncMessage");
                        InternalPCJ.getNetworker().send(getPhysicalMaster(), syncMessage); // goes to node 0 for "normal" barrier
//                         System.out.println("barrier] syncMessage sent");
                    } catch (IOException e) {
                        throw new RuntimeException("Node 0 failed!");
                    }
                    localSync.clear();
                }
                if (!unlocked) {
                    Lock.readUnlock();
                    unlocked = true;
                }
//                System.out.println("barrier] will await");
                syncObject.await();
//                System.out.println("barrier] after await");
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            InternalPCJ.getBarrierHandler().resetBarrier();
//             System.out.println("barrier] after resetBarrier");
            syncObject.unlock();
//             System.out.println("barrier] after unlock");
        } finally {
            if (!unlocked) {
//                 System.out.println("barrier] will finally unlock");
                Lock.readUnlock();
//                 System.out.println("barrier] finally unlocked");
            }
        }
    }


//    protected void barrierIfNoFailSinceLastBarrier(int myNodeId) {       // mstodo it seems to be not the place to  handle it
//        Lock.readLock();
//        boolean unlocked = false;
////        LogUtils.log(myNodeId, "barrier] lock"); // todo virtual node in log
//
//        try {
//            syncObject.lock();
//            InternalPCJ.getBarrierHandler().setGroupUnderBarrier(groupId);
//            try {
//                localSync.set(myNodeId);
////                LogUtils.log(myNodeId, "barrier] set localSync");
//                if (localSync.isSet(localSyncMask)) {
//                    // LogUtils.log(myNodeId, "barrier] localSync is set");
//                    Lock.readUnlock();
//                    unlocked = true;
//                    try {
//                        // LogUtils.log(myNodeId, "barrier] sending syncMessage");
//                        InternalPCJ.getNetworker().send(getPhysicalMaster(), syncMessage); // goes to node 0 for "normal" barrier
//                    } catch (IOException e) {
//                        throw new RuntimeException("Node 0 failed!");
//                    }
//                    localSync.clear();
//                }
//                if (!unlocked) {
//                    Lock.readUnlock();
//                    unlocked = true;
//                }
//                // LogUtils.log(myNodeId, "barrier] will await");
//                syncObject.await();
//            } catch (InterruptedException ex) {
//                throw new RuntimeException(ex);
//            }
//            InternalPCJ.getBarrierHandler().resetBarrier();
//            syncObject.unlock();
//        } finally {
//            if (!unlocked) {
//                Lock.readUnlock();
//            }
//        }
//    }

    /**
     * Synchronize current node and node with specified group nodeId
     *
     * @param nodeId group node id
     */
    protected void barrier(int myNodeId, int nodeId) { // mstodo fault tolerance  !!
        // FIXME: trzeba sprawdzić jak to będzie działać w pętli.
//        if (true) {
//            sync(myNodeId, new int[]{nodeId});
//        }

        /* change current group nodeId to global nodeId */
        nodeId = nodes.get(nodeId);
        myNodeId = nodes.get(myNodeId);

        try {
            MessageThreadPairSync msg = new MessageThreadPairSync();
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
        if (!nodes.containsKey(nodeId)) {
            throw new NodeFailedException();
        }
        nodeId = nodes.get(nodeId);

        MessageValueAsyncGetRequest msg = new MessageValueAsyncGetRequest();
        msg.setSenderGlobalNodeId(myNodeId);
        msg.setReceiverGlobalNodeId(nodeId);
        msg.setIndexes(indexes);
        msg.setVariableName(variable);

        InternalPCJ.getWorkerData().attachmentMap.put(msg.getMessageId(), futureObject);
        try {
            InternalPCJ.getFutureHandler().registerFutureObject(futureObject, nodeId);
            InternalPCJ.getNetworker().send(nodeId, msg);
            return futureObject;
        } catch (IOException ex) {
            FaultTolerancePolicy faultTolerancePolicy = InternalPCJ.getWorkerData().getFaultTolerancePolicy();
            faultTolerancePolicy.reportError(nodeId, true);
            throw new NodeFailedException(ex);
        }
    }

    protected void put(int nodeId, String variable, Object newValue, int... indexes) {
        Integer pNodeId = nodes.get(nodeId);      // mstodo nullpointer :O

        if (pNodeId == null) {
            throw new NodeFailedException(nodeId);
        }
        MessageValuePut msg = new MessageValuePut();
        msg.setReceiverGlobalNodeId(pNodeId);
        msg.setVariableName(variable);
        msg.setIndexes(indexes);
        msg.setVariableValue(CloneObject.serialize(newValue));

        try {
            InternalPCJ.getNetworker().send(pNodeId, msg);
        } catch (IOException ex) {
            FaultTolerancePolicy faultTolerancePolicy = InternalPCJ.getWorkerData().getFaultTolerancePolicy();
            faultTolerancePolicy.reportError(pNodeId, true);
            throw new NodeFailedException(ex);
        }
    }

    protected <T> InternalFutureObject<T> cas(InternalFutureObject<T> futureObject, int myNodeId, int nodeId, String variable, T expectedValue, T newValue, int... indexes) {
        nodeId = nodes.get(nodeId);

        MessageValueCompareAndSetRequest msg = new MessageValueCompareAndSetRequest();
        msg.setSenderGlobalNodeId(myNodeId);
        msg.setReceiverGlobalNodeId(nodeId);
        msg.setIndexes(indexes);
        msg.setVariableName(variable);
        msg.setExpectedValue(CloneObject.serialize(expectedValue));
        msg.setNewValue(CloneObject.serialize(newValue));

        InternalPCJ.getWorkerData().attachmentMap.put(msg.getMessageId(), futureObject);
        try {
            InternalPCJ.getNetworker().send(nodeId, msg);
            return futureObject;
        } catch (IOException ex) {
            throw new NodeFailedException(ex);
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
            throw new NodeFailedException(ex);
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
            throw new NodeFailedException(ex);
        }

    }

    public void removePhysicalNode(int physicalNodeId, Set<Integer> virtualNodes) {   // mstodo not invoked on node 0!!!
        // mstodo on other nodes there's no physicalIds!!
        int removedNodeIdx = physicalIds.indexOf(physicalNodeId);
        if (removedNodeIdx == -1) {
            System.out.println("Cannot remove from group " + this);
//            LogUtils.log(InternalPCJ.getWorkerData().physicalId, "No physical node with id: " + physicalNodeId + " found" + "\tphysical node ids: " + physicalIds);
        } else {
            System.out.println("Removing from group " + this);
            physicalIds.remove(removedNodeIdx);

            List<Integer> groupIdsToRemove = new ArrayList<>();
            for (Map.Entry<Integer, Integer> nodeEntry : nodes.entrySet()) {
                if (virtualNodes.contains(nodeEntry.getValue())) {
                    groupIdsToRemove.add(nodeEntry.getKey());
                }
            }

            for (Integer groupId : groupIdsToRemove) {
                nodes.remove(groupId);
            }

            physicalCommunication.removeNode(removedNodeIdx);

            localSync = localSync.without(removedNodeIdx);
            localSyncMask = localSyncMask.without(removedNodeIdx);
            physicalSync = physicalSync.without(removedNodeIdx);
        }
//
//
//        if (!removedNodes.contains(physicalNodeId)) {
//            int removedNodeIdx = physicalIds.indexOf(physicalNodeId);
//            if (removedNodeIdx > -1) {
//                physicalIds.remove(removedNodeIdx);
//
//                localSync = localSync.without(removedNodeIdx);     // mstodo what should be removed here???
//                localSyncMask = localSyncMask.without(removedNodeIdx);
//                physicalSync = physicalSync.without(removedNodeIdx);
//            }
//
//            physicalCommunication.removeNode(physicalNodeId);
//
//            List<Integer> groupIdsToRemove = new ArrayList<>();
//            for (Map.Entry<Integer, Integer> nodeEntry : nodes.entrySet()) {
//                if (virtualNodes.contains(nodeEntry.getValue())) {
//                    groupIdsToRemove.add(nodeEntry.getKey());
//                }
//            }
//
//            for (Integer groupId : groupIdsToRemove) {
//                nodes.remove(groupId);
//            }
//
//            removedNodes.add(physicalNodeId);
//        }
    }

    public void updateCommunicationTree(SetChild update) {
        switch (update.getDirection()) {
            case LEFT:
                System.out.println("new LEFT: " + update.getChild());  // mstodo remove
                setPhysicalLeft(update.getChild());
                break;
            case RIGHT:
                System.out.println("new RIGHT: " + update.getChild());  // mstodo remove
                setPhysicalRight(update.getChild());
                break;
        }
    }

    public void printCommunicationTree() {
        System.out.println(physicalCommunication.toString());
    }
}

