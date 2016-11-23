package org.pcj.internal;

import org.pcj.internal.faulttolerance.FaultTolerancePolicy;
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
    private final FaultTolerancePolicy policy;

    public FutureHandler(FaultTolerancePolicy policy) {
        this.policy = policy;
    }

    public synchronized void registerFutureObject(InternalFutureObject<?> internalFutureObject, int nodeId) {
        List<InternalFutureObject<?>> internalFutureObjects = getFutureObjects(nodeId);
        internalFutureObjects.add(internalFutureObject);
        futuresReverse.put(internalFutureObject, nodeId);
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
    public synchronized void unregisterFutureObject(InternalFutureObject<?> internalFutureObject) {
        Integer nodeId = futuresReverse.remove(internalFutureObject);
        futures.get(nodeId).remove(internalFutureObject);
    }

    public void nodeFailed(Integer nodeId) { // mstodo verify physical node id and thread id are not confused here!!!
        List<InternalFutureObject<?>> internalFutureObjects = getFutureObjects(nodeId);
        for (InternalFutureObject<?> InternalFutureObject : internalFutureObjects) {
            InternalFutureObject.fail(new NodeFailedException()); // mstodo add info about failed threads/nodes
        }
        internalFutureObjects.clear();
    }

}
