package org.pcj.internal.faulttolerance;

import org.pcj.PCJ;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.WorkerData;
import org.pcj.internal.message.BroadcastedMessage;
import org.pcj.internal.message.MessageFinished;
import org.pcj.internal.message.MessageNodeFailed;
import org.pcj.internal.message.MessageNodeRemoved;
import org.pcj.internal.message.MessageSyncGo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedList;
import static org.pcj.internal.InternalPCJ.getBarrierHandler;
import static org.pcj.internal.InternalPCJ.getFutureHandler;
import static org.pcj.internal.InternalPCJ.getNetworker;
import static org.pcj.internal.InternalPCJ.getWorkerData;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/25/15
 * Time: 10:43 PM
 */
public class IgnoreFaultTolerancePolicy implements FaultTolerancePolicy {

    /**
     * nodes for which failure has been discovered
     */
    private Set<Integer> failedNodes = new HashSet<>();
    private Set<Integer> failedThreads = new HashSet<>();
    private Set<Consumer<NodeFailedException>> failureHandlers = new HashSet<>();

    /**
     * set of failed nodes unknown for the application
     */
    private final Set<Integer> newFailures = new HashSet<>();    // mstodo too early ? won't it miss
    private final List<Thread> threadsWaitingForFailure = synchronizedList(new ArrayList<>());
    private final NodeFailureWaiter nodeFailureWaiter = new NodeFailureWaiter();

    @Override
    public void handleNodeFailure(int failedNodeId) {
        Lock.writeLock();
        List<SetChild> updates;
        try {
            if (failedNodes.contains(failedNodeId)) {
                return;
            }
            failedNodes.add(failedNodeId);
//         System.out.println("[" + new Date() + "]Acquiring write lock for node removal: " + failedNodeId + "...");
//         System.out.println("[" + new Date() + "]DONE");
            failedThreads.addAll(getWorkerData().getVirtualNodes(failedNodeId));
            updates = InternalPCJ.getNode0Data().remove(failedNodeId);

            mockNodeFinish(getWorkerData());
//         System.out.println("after mocking node finish"); // mstodo remove
        } finally {
            Lock.writeUnlock();
        }

        Set<Integer> physicalNodes = getWorkerData().getPhysicalNodes().keySet();   // todo: is synchronization needed?
//        System.out.println("()()()   physical nodes: " + physicalNodes);
        propagateFailure(failedNodeId, updates);
//        for (Integer node : physicalNodes) {
//            sending to the failed node will fail
//            System.out.println("sending to " + node + " for failure of: " + failedNodeId);
//            if (!node.equals(failedNodeId)) {
//            }
//        }
//      System.out.println("failure propagated"); // mstodo remove

    }

    @Override
    public void reportError(int nodeId, boolean waitForReconfiguration) {
        System.err.println("reporting error for nodeId: " + nodeId + ", will wait for reconfiguration: " + waitForReconfiguration);
        Thread.dumpStack();
        System.err.println(asList(Thread.currentThread().getStackTrace()));
        MessageNodeFailed message = new MessageNodeFailed(nodeId);
        try {
            if (PCJ.getFailedNodeIds().contains(nodeId)) {
                return;
            }
            InternalPCJ.getNetworker().sendToPhysicalNode(0, message);
        } catch (IOException e) {
            e.printStackTrace();

            System.err.println("Node 0 failed. Quitting JVM");
            System.exit(12);
        }

        if (waitForReconfiguration) {
            nodeFailureWaiter.waitForFailure(nodeId);   // mstodo sth wrong on node 0 - maybe if both children are dead?
        }
        System.out.println("Finished reporting error");
    }

    @Override
    public void error(MessageNodeRemoved message) {
        Lock.writeLock();
        try {
            int failedNodeId = message.getFailedNodePhysicalId();
            System.out.println("GOT NODE REMOVED: " + failedNodeId);
            WorkerData data = getWorkerData();
            data.removePhysicalNode(failedNodeId);

            int myNodeId = data.getPhysicalId();
            for (SetChild update : message.getCommunicationUpdates()) {
                if (update.getParent().equals(myNodeId)) {
                    data.getInternalGlobalGroup().updateCommunicationTree(update);
                    replayBroadcast();
                }
                if (update.isNewChild(myNodeId)) {
                    data.getInternalGlobalGroup().setPhysicalParent(update.getParent());
                }
            }
            data.addFailedNode(failedNodeId);

            failureHandlers.forEach(h -> h.accept(new NodeFailedException(failedNodeId)));   // mstodo is it needed?
            synchronized (newFailures) {
                newFailures.add(failedNodeId);
            }
            System.out.println("INTERRUPTING WAITING FOR THREADS"); //mstodo remove
            threadsWaitingForFailure.forEach(Thread::interrupt);

            getFutureHandler().nodeFailed(failedNodeId);
            nodeFailureWaiter.nodeFailed(failedNodeId);
            try {
                getBarrierHandler().finishBarrierIfInProgress(failedThreads);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } finally {
            Lock.writeUnlock();
        }
    }

    @Override
    public void register(Thread thread) {
        threadsWaitingForFailure.add(thread);
        if (!newFailures.isEmpty()) {
            System.out.println("INTERRUPTING WAITING FOR THREADS"); //mstodo remove
            threadsWaitingForFailure.forEach(t -> t.interrupt());
        }
    }

    @Override
    public void unregister(Thread thread) {
        threadsWaitingForFailure.remove(thread);
    }

    @Override
    public void failOnNewFailure() {
        if (!newFailures.isEmpty()) {
            synchronized (newFailures) {
                List<Integer> failures = new ArrayList<>(newFailures);
                newFailures.clear();
                throw new NodeFailedException(failures);
            }
        }
    }

    private void replayBroadcast() {
        WorkerData data = getWorkerData();
        List<BroadcastedMessage> list = data.getBroadcastCache().getList();
        MessageSyncGo lastSyncGo = null; // mstodo undo
        for (BroadcastedMessage message : list) {
            if (message instanceof MessageSyncGo) {
                lastSyncGo = (MessageSyncGo) message;
            } else {
                getNetworker().broadcast(message);
            }
        }

        if (lastSyncGo != null) {
            getNetworker().broadcast(lastSyncGo);
        }
    }

//    private void finishBarrierIfInProgress(int failedNodeId) {  // mstodo move to the new mechanism
//        try {
//            getBarrierHandler().finishBarrierIfInProgress(failedNodeId, failedThreads);
//         System.out.println("barrier should be released");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void mockNodeFinish(WorkerData data) {
        try {
            InternalPCJ.getNetworker().send(data.getInternalGlobalGroup(), new MessageFinished());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // mstodo broadcast it!
    private void propagateFailure(int failedNodeId, List<SetChild> updates) {
        try {
//            LogUtils.setEnabled(true);
            MessageNodeRemoved message = new MessageNodeRemoved(failedNodeId);
            message.setCommunicationUpdates(updates);
            InternalPCJ.getNetworker().send(getWorkerData().getInternalGlobalGroup(), message);
//            InternalPCJ.getNetworker().sendToPhysicalNode(node, message);
        } catch (IOException e) {
            System.err.println("Error trying to broadcast message for failed node: " + failedNodeId);
            e.printStackTrace();
        }
    }

}
