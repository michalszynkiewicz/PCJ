package org.pcj.internal.faulttolerance;

import org.pcj.PCJ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 2/2/16
 * Time: 8:59 PM
 */
public class NodeFailureWaiter {
   private static final Map<Integer, List<Object>> waitObjects = new ConcurrentHashMap<>();

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
      List<Object> objects = waitObjects.computeIfAbsent(nodeId, k -> new ArrayList<>());
      Object waitObject = new Object();
      objects.add(waitObject);
      return waitObject;
   }

   public synchronized void nodeFailed(int failedNodeId) {
      List<Object> objects = waitObjects.get(failedNodeId);
      for (Object waitObject : objects == null ? emptyList() : objects) {
         synchronized (waitObject) {
            waitObject.notifyAll();
         }
      }
   }
}
