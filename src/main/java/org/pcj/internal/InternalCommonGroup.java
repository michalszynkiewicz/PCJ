/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal;

import org.pcj.PCJ;
import org.pcj.internal.ft.SetChild;
import org.pcj.internal.futures.GroupBarrierState;
import org.pcj.internal.futures.GroupJoinState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.pcj.PCJ.getNodeId;
import static org.pcj.StreamUtils.ONE_MATCHES;

/**
 * Internal (with common ClassLoader) representation of Group. It contains
 * common data for groups.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class InternalCommonGroup {

    public static final int GLOBAL_GROUP_ID = 0;
    public static final String GLOBAL_GROUP_NAME = "";

    private final ConcurrentMap<Integer, Integer> threadsMapping; // groupThreadId, globalThreadId
    private final Object joinGroupSynchronizer;
    private final int groupId;
    private final String groupName;
    private final List<Integer> localIds;
    private final List<Integer> physicalIds;
    private final Bitmask localBitmask;
    private final Bitmask physicalBitmask;
    private final ConcurrentMap<Integer, GroupBarrierState> barrierStateMap;
    private final ConcurrentMap<List<Integer>, GroupJoinState> groupJoinStateMap;
    final private AtomicInteger threadsCounter;
    final private CommunicationTree physicalTree;

    public InternalCommonGroup(InternalCommonGroup g) {
        this.groupId = g.groupId;
        this.groupName = g.groupName;
        this.physicalTree = g.physicalTree;

        this.threadsMapping = g.threadsMapping;
        this.localBitmask = g.localBitmask;
        this.physicalBitmask = g.physicalBitmask;
        this.barrierStateMap = g.barrierStateMap;

        this.groupJoinStateMap = g.groupJoinStateMap;

        this.localIds = g.localIds;
        this.physicalIds = g.physicalIds;

        this.threadsCounter = g.threadsCounter;
        this.joinGroupSynchronizer = g.joinGroupSynchronizer;
    }

    public InternalCommonGroup(int groupMaster, int groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        physicalTree = new CommunicationTree(groupMaster);

        threadsMapping = new ConcurrentHashMap<>();

        localBitmask = new Bitmask();
        physicalBitmask = new Bitmask();
        barrierStateMap = new ConcurrentHashMap<>();
        groupJoinStateMap = new ConcurrentHashMap<>();

        localIds = new ArrayList<>();
        physicalIds = new CopyOnWriteArrayList<>();
        updateCommunicationTree(groupMaster);

        threadsCounter = new AtomicInteger(0);
        joinGroupSynchronizer = new Object();
    }

    final protected int getGroupId() {
        return groupId;
    }

    final public String getGroupName() {
        return groupName;
    }

    final public int getGroupMasterNode() {
        return physicalTree.getRootNode();
    }

    final public int getParentNode() {
        return physicalTree.getParentNode();
    }

    final public List<Integer> getChildrenNodes() {
        return physicalTree.getChildrenNodes();
    }

    final protected Bitmask getPhysicalBitmask() {
        return new Bitmask(physicalBitmask);
    }

    protected int myId() {
        throw new IllegalStateException("This method has to be overriden!");
    }

    final public int threadCount() {
        return threadsMapping.size();
    }

    final public int[] getLocalThreadsId() {
        return localIds.stream().mapToInt(Integer::intValue).toArray();
    }

    final public int getGlobalThreadId(int groupThreadId) throws NoSuchElementException {
        Integer globalThreadId = threadsMapping.get(groupThreadId);
        if (globalThreadId == null) {
            throw new NoSuchElementException("Group threadId not found: " + groupThreadId);
        }
        return globalThreadId;
    }

    final public int getGroupThreadId(int globalThreadId) throws NoSuchElementException {
        if (threadsMapping.containsValue(globalThreadId)) {
            for (Map.Entry<Integer, Integer> entry : threadsMapping.entrySet()) {
                if (entry.getValue() == globalThreadId) {
                    return entry.getKey();
                }
            }
        }
        throw new NoSuchElementException("Global threadId not found: " + globalThreadId);
    }

    final public int addNewThread(int globalThreadId) {
        int groupThreadId;
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        synchronized (joinGroupSynchronizer) {
            do {
                groupThreadId = threadsCounter.getAndIncrement();
            } while (threadsMapping.putIfAbsent(groupThreadId, globalThreadId) != null);

            updateCommunicationTree(physicalId);
            updateLocalBitmask(physicalId, groupThreadId);
        }

        return groupThreadId;
    }

    final public void addThread(int globalThreadId, int groupThreadId) {
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        synchronized (joinGroupSynchronizer) {
            if (threadsMapping.putIfAbsent(groupThreadId, globalThreadId) != null) {
                return;
            }

            updateCommunicationTree(physicalId);
            updateLocalBitmask(physicalId, groupThreadId);
        }

    }

    private void updateCommunicationTree(int physicalId) {
        if (physicalIds.contains(physicalId)) {
            return;
        }

        physicalIds.add(physicalId);
        int index = physicalIds.lastIndexOf(physicalId);
        if (index > 0) {
            int currentPhysicalId = InternalPCJ.getNodeData().getPhysicalId();
            int parentId = physicalIds.get((index - 1) / 2);

            if (currentPhysicalId == physicalId) {
                physicalTree.setParentNode(parentId);
            }
            if (currentPhysicalId == parentId) {
                physicalTree.getChildrenNodes().add(physicalId);
            }
        }

        physicalBitmask.enlarge(physicalId + 1);
        physicalBitmask.set(physicalId);
    }

    private void updateLocalBitmask(int physicalId, int groupThreadId) {
        int currentPhysicalId = InternalPCJ.getNodeData().getPhysicalId();

        if (physicalId == currentPhysicalId) {
            localIds.add(groupThreadId);
            localBitmask.enlarge(groupThreadId + 1);
            localBitmask.set(groupThreadId);
        }
    }

    public Map<Integer, Integer> getThreadsMapping() {
        return Collections.unmodifiableMap(threadsMapping);
    }

    final protected GroupBarrierState barrier(int threadId, int barrierRound) {
        GroupBarrierState barrierState = getBarrierState(barrierRound);
        System.out.println("[" + PCJ.getNodeId() + "/" + threadId + "] reached barrier " + barrierRound + ", " + barrierState);
        barrierState.processLocal(threadId);

        return barrierState;
    }

    final public GroupBarrierState getBarrierState(int barrierRound) {
        return barrierStateMap.computeIfAbsent(barrierRound,
                round -> new GroupBarrierState(groupId, round, localBitmask, getChildrenNodes()));
    }

    final public GroupBarrierState removeBarrierState(int barrierRound) {
        return barrierStateMap.remove(barrierRound);
    }

    public GroupJoinState getGroupJoinState(int requestNum, int threadId, List<Integer> childrenNodes) {
        return groupJoinStateMap.computeIfAbsent(Arrays.asList(requestNum, threadId),
                key -> new GroupJoinState(groupId, requestNum, threadId, childrenNodes));
    }

    public GroupJoinState removeGroupJoinState(int requestNum, int threadId) {
        return groupJoinStateMap.remove(Arrays.asList(requestNum, threadId));
    }

    public CommunicationTree getPhysicalTree() {
        return physicalTree;
    }

    public boolean removeNode(Integer physicalId, Collection<Integer> threadIds, Collection<SetChild> treeUpdates) {
        // mstodo: finish
        boolean removed = physicalIds.remove(physicalId) || localIds.removeAll(threadIds);

        if (removed) {
            if (getGroupMasterNode() == getNodeId()) {
                barrierStateMap.values()
                        .forEach(state -> updateBarrierState(state, physicalId, treeUpdates));
            }
            List<Integer> failedGroupThreadIds = threadsMapping.entrySet().stream()
                    .filter(e -> threadIds.contains(e.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            failedGroupThreadIds.forEach(threadsMapping::remove);
        }

        return removed;
    }

    private void updateBarrierState(GroupBarrierState state, Integer physicalId, Collection<SetChild> treeUpdates) {
        state.processPhysical(physicalId);
        treeUpdates.stream()
                .filter(update -> update.getParent().equals(PCJ.getNodeId()))
                .map(SetChild::getChild)
                .forEach(state::addChild);
    }

    /**
     * if this node of the communication tree is involved in any update
     * required to fix the tree, apply the updates.
     *
     * @param communicationUpdates list of all updates, not only corresponding to this node
     * @return true if one of the children of current node was modified
     */
    public boolean applyTreeUpdates(List<SetChild> communicationUpdates) {
        return communicationUpdates.stream().map(
                update -> {
                    if (update.touches(getNodeId())) {
                        if (update.isNewChild(getNodeId())) {
                            physicalTree.setParentNode(update.getParent());
                        } else if (update.getParent().equals(getNodeId())) {
                            updateChild(update);
                            return true;
                        }
                    }
                    return false;
                }
                // if there's any true - return true
        ).reduce(false, ONE_MATCHES);
    }

    private void updateChild(SetChild update) {
        Integer newChild = update.getChild();
        List<Integer> children = physicalTree.getChildrenNodes();
        int childIndex = update.getDirection().ordinal();
        while (childIndex >= children.size()) {
            children.add(null);
        }

        physicalTree.getChildrenNodes().set(childIndex, newChild);

        while (!children.isEmpty() && children.get(children.size() - 1) == null) {
            children.remove(children.size() - 1);
        }
    }

    public Collection<Integer> getThreadIds() {
        return threadsMapping.values();
    }

    public ConcurrentMap<Integer, GroupBarrierState> getBarrierStateMap() {
        return barrierStateMap;
    }
}
