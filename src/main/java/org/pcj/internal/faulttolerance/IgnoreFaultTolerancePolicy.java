package org.pcj.internal.faulttolerance;

import org.pcj.PCJ;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.WorkerData;
import org.pcj.internal.message.MessageFinished;
import org.pcj.internal.message.MessageNodeFailed;
import org.pcj.internal.message.MessageNodeRemoved;

import java.io.IOException;
import java.util.*;

import static java.util.Arrays.asList;
import static org.pcj.internal.InternalPCJ.getBarrierHandler;
import static org.pcj.internal.InternalPCJ.getWorkerData;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/25/15
 * Time: 10:43 PM
 */
public class IgnoreFaultTolerancePolicy implements FaultTolerancePolicy {

   private Set<Integer> failedNodes = new HashSet<>();
   private Set<Integer> failedThreads = new HashSet<>();

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
         finishBarrierIfInProgress(failedNodeId);

         mockNodeFinish(getWorkerData());
//         System.out.println("after mocking node finish"); // mstodo remove
      } finally {
         Lock.writeUnlock();
      }

      Set<Integer> physicalNodes = getWorkerData().getPhysicalNodes().keySet();   // todo: is synchronization needed?
      System.out.println("()()()   ");
      System.out.println("()()()   ");
      System.out.println("()()()   physical nodes: " + physicalNodes);
      System.out.println("()()()   ");
      System.out.println("()()()   ");
      for (Integer node : physicalNodes) {
//            sending to the failed node will fail
          System.out.println("sending to " + node +" for failure of: " + failedNodeId);
         if (!node.equals(failedNodeId)) {
            propagateFailure(failedNodeId, node, updates);
         }
      }
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
         InternalPCJ.getNodeFailureWaiter().waitForFailure(nodeId);   // mstodo sth wrong on node 0 - maybe if both children are dead?
      }
      System.out.println("Finished reporting error");
   }

   private void finishBarrierIfInProgress(int failedNodeId) {
      try {
         getBarrierHandler().finishBarrierIfInProgress(failedNodeId, failedThreads);
//         System.out.println("barrier should be released");
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private void mockNodeFinish(WorkerData data) {
      try {
         InternalPCJ.getNetworker().send(data.getInternalGlobalGroup(), new MessageFinished());
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private void propagateFailure(int failedNodeId, Integer node, List<SetChild> updates) {
      try {
//            LogUtils.setEnabled(true);
         MessageNodeRemoved message = new MessageNodeRemoved(failedNodeId);
         message.setCommunicationUpdates(updates);
         InternalPCJ.getNetworker().sendToPhysicalNode(node, message);
      } catch (IOException e) {
         System.err.println("Error trying to send message to node: " + node + " for failed node: " + failedNodeId);
         e.printStackTrace();
      }
   }

}
