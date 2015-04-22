package org.pcj.internal;

import org.pcj.internal.faulttolerance.NodeFailedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/8/15
 * Time: 10:28 PM
 */
public class FutureHandler {

    private final Map<Integer, List<InternalFutureObject<?>>> futures = new HashMap<>();
    private final Map<InternalFutureObject<?>, Integer> futuresReverse = new HashMap<>();  // not to scan whole futures map when unregistering

    public synchronized void registerFutureObject(InternalFutureObject<?> InternalFutureObject, int nodeId) {
        List<InternalFutureObject<?>> InternalFutureObjects = getFutureObjects(nodeId);
        InternalFutureObjects.add(InternalFutureObject);
    }

    private List<InternalFutureObject<?>> getFutureObjects(int nodeId) {
        List<InternalFutureObject<?>> InternalFutureObjects = futures.get(nodeId);
        if (InternalFutureObjects == null) {
            InternalFutureObjects = new ArrayList<>();
            futures.put(nodeId, InternalFutureObjects);
        }
        return InternalFutureObjects;
    }

    // mstoodo test if we need synchronization - Lock should handle it
    public synchronized void unregisterFutureObject(InternalFutureObject<?> InternalFutureObject) {
        Integer nodeId = futuresReverse.remove(InternalFutureObject);
        futures.get(nodeId).remove(InternalFutureObject);
    }

    public void nodeFailed(Integer nodeId) { // mstodo verify physical node id and thread id are not confused here!!!
        List<InternalFutureObject<?>> InternalFutureObjects = getFutureObjects(nodeId);
        for (InternalFutureObject<?> InternalFutureObject : InternalFutureObjects) {
            InternalFutureObject.fail(new NodeFailedException()); // mstodo add info about failed threads/nodes
        }
        InternalFutureObjects.clear();
    }

}
