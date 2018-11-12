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
import org.pcj.internal.futures.BroadcastState;
import org.pcj.internal.futures.GroupBarrierState;
import org.pcj.internal.futures.GroupJoinState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ConcurrentMap<Integer, Integer> activeThreadsMapping; // above excluding failed threads
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
    private final ConcurrentMap<Integer, BroadcastState> broadcastStateMap;
    private final Set<InternalGroup> threadGroups;

    public InternalCommonGroup(int groupMaster, int groupId, String groupName) {
        this.threadGroups = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.groupId = groupId;
        this.groupName = groupName;
        physicalTree = new CommunicationTree(groupMaster);

        threadsMapping = new ConcurrentHashMap<>();
        activeThreadsMapping = new ConcurrentHashMap<>();

        localBitmask = new Bitmask();
        physicalBitmask = new Bitmask();
        barrierStateMap = new ConcurrentHashMap<>();
        groupJoinStateMap = new ConcurrentHashMap<>();

        localIds = new ArrayList<>();
        physicalIds = new CopyOnWriteArrayList<>();
        updateCommunicationTree(groupMaster);

        threadsCounter = new AtomicInteger(0);
        joinGroupSynchronizer = new Object();
        this.broadcastStateMap = new ConcurrentHashMap<>();
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
        return activeThreadsMapping.size();
    }

    final public int[] getLocalThreadsId() {
        return localIds.stream().mapToInt(Integer::intValue).toArray();
    }

    final public int getGlobalThreadId(int groupThreadId) throws NoSuchElementException {
        Integer globalThreadId = activeThreadsMapping.get(groupThreadId);
        if (globalThreadId == null) {
            throw new NoSuchElementException("Group threadId not found: " + groupThreadId);
        }
        return globalThreadId;
    }

    final public int getGlobalThreadIdInclFailed(int groupThreadId) {
        return Optional.ofNullable(threadsMapping.get(groupThreadId))
                .orElseThrow(() -> new NoSuchElementException("Group threadId not found: " + groupThreadId));
    }

    final public int getGroupThreadId(int globalThreadId) throws NoSuchElementException {
        if (activeThreadsMapping.containsValue(globalThreadId)) {
            for (Map.Entry<Integer, Integer> entry : activeThreadsMapping.entrySet()) {
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
            } while (activeThreadsMapping.putIfAbsent(groupThreadId, globalThreadId) != null);

            threadsMapping.putAll(activeThreadsMapping);// TODO: mods required on adding threads

            updateCommunicationTree(physicalId);
            updateLocalBitmask(physicalId, groupThreadId);
        }

        return groupThreadId;
    }

    final public void addThread(int globalThreadId, int groupThreadId) {
        int physicalId = InternalPCJ.getNodeData().getPhysicalId(globalThreadId);
        synchronized (joinGroupSynchronizer) {
            if (activeThreadsMapping.putIfAbsent(groupThreadId, globalThreadId) != null) {
                return;
            }
            threadsMapping.putAll(activeThreadsMapping);// TODO: mods required on adding threads

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
        return Collections.unmodifiableMap(activeThreadsMapping);
    }

    final protected GroupBarrierState barrier(int threadId, int barrierRound) {
        GroupBarrierState barrierState = getBarrierState(barrierRound, true);
        barrierState.processLocal(threadId);

        return barrierState;
    }

    final public GroupBarrierState getBarrierState(int barrierRound, boolean isInitializing) {
        if (!isInitializing && barrierRound <= getLatestBarrierRound()){
            return barrierStateMap.get(barrierRound);
        } else {
            return barrierStateMap.computeIfAbsent(barrierRound,
                    round -> new GroupBarrierState(groupId, round, localBitmask, getChildrenNodes()));
        }
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
        System.out.println("invoked removeNode, removed: " + removed);

        if (removed) {
            if (getGroupMasterNode() == getNodeId()) {
                barrierStateMap.values()
                        .forEach(state -> updateBarrierState(state, physicalId, treeUpdates));
            }

            System.out.println("[" + PCJ.getNodeId() + "] will remove states");
            // mstodo might be good to add an exception to the broadcast state
            
            broadcastStateMap.values()
                    .stream()
                    .peek(state -> System.out.println("[" + PCJ.getNodeId() + "] processing state: " + state))
                    .forEach(state -> state.processPhysical(physicalId));

            physicalBitmask.clear(physicalId);

            threadsMapping.entrySet().stream()
                    .filter(e -> threadIds.contains(e.getValue()))
                    .map(Map.Entry::getKey)
                    .forEach(activeThreadsMapping::remove);

            threadGroups.forEach(group -> group.removeNode(threadIds));
        }

        return removed;
    }

    private void updateBarrierState(GroupBarrierState state, Integer physicalId, Collection<SetChild> treeUpdates) {
        state.processPhysical(physicalId);
        treeUpdates.stream()
                .filter(update -> update.getParent().equals(PCJ.getNodeId()))
                .map(SetChild::getChild)
                .peek(child -> System.out.println("[" + PCJ.getNodeId() + "] adding to barrier a child " + child))
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
                    System.out.println("[" + getNodeId() + "] applying tree updates");
                    if (update.touches(getNodeId())) {
                        System.out.println("[" + getNodeId() + "] touched by " + update);
                        if (update.isNewChild(getNodeId())) {
                            physicalTree.setParentNode(update.getParent());
                            System.out.println("[" + getNodeId() + "] I'ma new child ");
                            return true;
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
        System.out.println("[" + PCJ.getNodeId() + "] glogro set new child: " + newChild);

        while (!children.isEmpty() && children.get(children.size() - 1) == null) {
            children.remove(children.size() - 1);
        }
        System.out.println("[" + PCJ.getNodeId() + "] nodes after update: " + getChildrenNodes());
    }

    public Collection<Integer> getThreadIds() {
        return threadsMapping.values();
    }

    public ConcurrentMap<Integer, GroupBarrierState> getBarrierStateMap() {
        return barrierStateMap;
    }

    public void markBarrierReached(int latestReachedBarrier, boolean barrierFinished, int senderId) {
        barrierStateMap
                .forEach(
                        (round, barrier) -> {
                            if (round < latestReachedBarrier) {
                                barrier.processPhysical(senderId);
                            }
                            if (round == latestReachedBarrier && barrierFinished) {
                                barrier.processPhysical(senderId);
                            }
                        }
                );

        getBarrierState(latestReachedBarrier, false);

    }

    public Integer getLatestBarrierRound() {
        return threadGroups.stream()
                .map(InternalGroup::getLatestBarrierRound)
                .max(Integer::compare)
                .orElse(0);
    }

    public final BroadcastState getBroadcastState(int requestNum) {
        System.out.println("[" + PCJ.getNodeId() + "] getting broadcast state");
        BroadcastState state = broadcastStateMap.computeIfAbsent(requestNum,
                key -> {
                    System.out.println("adding new state");
                    return new BroadcastState(getPhysicalBitmask());
                });
        System.out.println("states amount: " + barrierStateMap.size());
        return state;
    }

    public BroadcastState removeBroadcastState(int requestNum) {
        return broadcastStateMap.remove(requestNum);
    }

    public void addThreadGroup(InternalGroup internalGroup) {
        threadGroups.add(internalGroup);
    }
}
