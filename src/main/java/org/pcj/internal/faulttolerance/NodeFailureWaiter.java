package org.pcj.internal.faulttolerance;

import org.pcj.PCJ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 2/2/16
 * Time: 8:59 PM
 */
public class NodeFailureWaiter {
   private static final Map<Integer, List<Object>> waitObjects = new ConcurrentHashMap<>();
   private final FaultTolerancePolicy policy;

   public NodeFailureWaiter(FaultTolerancePolicy policy) {
      this.policy = policy;
   }

   public void waitForFailure(Integer nodeId) {
      if (PCJ.getFailedNodeIds().contains(nodeId)) {
         return;
      }
      final Object o = getWaitObject(nodeId);
      Lock.releaseCurrentThreadLocks();
      synchronized (o) {
         try {
            o.wait();
         } catch (InterruptedException ignored) {
            ignored.printStackTrace();
         }
      }
   }

   private synchronized Object getWaitObject(Integer nodeId) {
      List<Object> objects = waitObjects.get(nodeId);
      if (objects == null) {
         objects = new ArrayList<>();
         waitObjects.put(nodeId, objects);
      }
      Object waitObject = new Object();
      objects.add(waitObject);
      return waitObject;
   }

   public synchronized void nodeFailed(int failedNodeId) {
      for (Object waitObject : waitObjects.get(failedNodeId)) {
         synchronized (waitObject) {
            waitObject.notifyAll();
         }
      }
   }
}
