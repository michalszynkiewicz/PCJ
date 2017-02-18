/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import org.pcj.internal.faulttolerance.BroadcastCache;
import org.pcj.internal.faulttolerance.FaultTolerancePolicy;
import org.pcj.internal.faulttolerance.IgnoreFaultTolerancePolicy;
import org.pcj.internal.faulttolerance.LazyActivityMonitor;
import org.pcj.internal.message.MessageHello;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.utils.PcjThreadPair;

import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class is used by Worker class for storing data.
 * 
 * At present this class can be used only in sequential manner.
 * 
 * In the future plan is to make this class thread-safe and
 * to implement Worker in such way, to operate concurrently.
 * 
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public final class WorkerData {

    /* for node0: */
    int physicalNodesCount;
    int clientsConnected;
    int helloCompletedCount;
    final int clientsCount;
    final Map<String, Integer> groupsIds; // key - groupName, value - groupId
    final Map<Integer, Integer> groupsMaster; // key - groupId, value - physicalId
    final Map<Integer, MessageHello> helloMessages;
    /* clients/nodes */
    final int[] localIds;
    /* connecting info */
    int physicalId;
    final Map<Integer, SocketChannel> physicalNodes;
    final Map<SocketChannel, Integer> physicalNodesIds;
    //final BlockingQueue<Message> pendingMessages;
    final Map<Integer, Integer> virtualNodes; // key = nodeId, value physicalId
    /* messages */
    final Map<Integer, Attachment> attachmentMap; // key = messageId, all messages have different id, even from different local virtualNodes
    /* groups */
    final InternalGroup internalGlobalGroup;
    final ConcurrentMap<Integer, InternalGroup> internalGroupsById; // key - groupId, value - group
    final ConcurrentMap<String, InternalGroup> internalGroupsByName; // key - groupName, value - group
    /* virtualNodes sync */
    final ConcurrentMap<NodesSyncData, NodesSyncData> nodesSyncData;
    final ConcurrentMap<Integer, PcjThreadLocalData> localData;
    final ConcurrentMap<PcjThreadPair, PcjThreadPair> syncNodePair; // key=nodeId, value=0,1,...??
    /* finish */
    final Object finishObject;

    final LazyActivityMonitor activityMonitor;
    final Set<Integer> failedThreadIds = new HashSet<>();
    private Set<Integer> failedNodeIds = new HashSet<>();
    final BroadcastCache broadcastCache;

    public WorkerData(int[] localIds, ConcurrentMap<Integer, PcjThreadLocalData> localData, InternalGroup globalGroup) {
        this(localIds, localData, globalGroup, null);
    }

    public WorkerData(int[] localIds, ConcurrentMap<Integer, PcjThreadLocalData> localData, InternalGroup globalGroup, Integer clientsCount) {
        if (clientsCount == null) { // as normal
            this.clientsCount = -1;
            this.clientsConnected = -1;
            physicalNodesCount = -1;

            groupsMaster = null;
            groupsIds = null;

            helloMessages = null;
        } else { // as manager -> clientsCount == null
            this.clientsCount = clientsCount;
            this.clientsConnected = 0;
            physicalNodesCount = 0;

            groupsIds = new ConcurrentHashMap<>();
            groupsIds.put("", 0);

            groupsMaster = new ConcurrentHashMap<>();
            groupsMaster.put(0, 0);

            helloMessages = Collections.synchronizedSortedMap(new TreeMap<>());
        }
        this.localIds = localIds;
        this.localData = localData;

        physicalId = -1;

        internalGlobalGroup = globalGroup;
        internalGroupsById = new ConcurrentHashMap<>();
        internalGroupsByName = new ConcurrentHashMap<>();
        addGroup(internalGlobalGroup);

        //pendingMessages = new LinkedBlockingQueue<>();

        virtualNodes = new ConcurrentHashMap<>();
        physicalNodes = new ConcurrentHashMap<>();
        physicalNodesIds = new ConcurrentHashMap<>();

        nodesSyncData = new ConcurrentHashMap<>();
        syncNodePair = new ConcurrentHashMap<>();

        attachmentMap = new ConcurrentHashMap<>();

        finishObject = new Object();
        activityMonitor = new LazyActivityMonitor(new IgnoreFaultTolerancePolicy());// mstodo children
        // mstodo 2 fault tolerance policy customization
        broadcastCache = new BroadcastCache();
    }

    void addGroup(InternalGroup group) {
        synchronized (internalGroupsById) {
            internalGroupsById.put(group.getGroupId(), group);
            internalGroupsByName.put(group.getGroupName(), group);
        }
    }

    Integer getPhysicalId(SocketChannel socket) {
        return (socket instanceof LoopbackSocketChannel)
                ? physicalId
                : physicalNodesIds.get(socket);
    }

    public void removePhysicalNode(int physicalNodeId) {
        // total physical nodes count is decreased elsewhere - via MessageFinished
        SocketChannel socketChannel = physicalNodes.get(physicalNodeId);

        physicalNodes.remove(physicalNodeId);
        physicalNodesIds.remove(socketChannel);

        Set<Integer> virtualNodesToRemove = new HashSet<>();
        for (Map.Entry<Integer, Integer> virtualToPhysicalNodeId : virtualNodes.entrySet()) {
            if (virtualToPhysicalNodeId.getValue().equals(physicalNodeId)){
                virtualNodesToRemove.add(virtualToPhysicalNodeId.getKey());
            }
        }

        for (Integer virtualNodeId : virtualNodesToRemove) {
            virtualNodes.remove(virtualNodeId);
        }

        for (InternalGroup group : internalGroupsById.values()) {
            group.removePhysicalNode(physicalNodeId, virtualNodesToRemove);
//            System.out.println("removed " + physicalNodeId + " from " + group);
        }
    }


    public Map<Integer, SocketChannel> getPhysicalNodes() {
        return physicalNodes;
    }

    public InternalGroup getInternalGlobalGroup() {
        return internalGlobalGroup;
    }

    public Set<Integer> getFailedThreadIds() {
        return Collections.unmodifiableSet(failedThreadIds);
    }

    public Set<Integer> getFailedNodeIds() {
        return Collections.unmodifiableSet(failedNodeIds);
    }

    public void addFailedNode(Integer nodeId) {
        failedThreadIds.addAll(getVirtualNodes(nodeId));
        failedNodeIds.add(nodeId);
    }

    public List<Integer> getVirtualNodes(int physicalNodeId) {
        List<Integer> nodes = new ArrayList<>();
        for (Map.Entry<Integer, Integer> vNodeEntry : virtualNodes.entrySet()) {
            if (vNodeEntry.getValue() == physicalNodeId) {
                nodes.add(vNodeEntry.getKey());
            }
        }
        return nodes;
    }

    public FaultTolerancePolicy getFaultTolerancePolicy() {
        return activityMonitor.getFaultTolerancePolicy();
    }

    public int getPhysicalId() {
        return physicalId;
    }

    public LazyActivityMonitor getActivityMonitor() {
        return activityMonitor;
    }

    public BroadcastCache getBroadcastCache() {
        return broadcastCache;
    }
}