package org.pcj.internal;

import org.pcj.internal.faulttolerance.NodeFailedException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 3/26/15
 * Time: 10:17 PM
 */
//mstodo analyze concurrency
public class WaitForHandler {

    private Set<Field> fieldsToAwake = new HashSet<>();
    private List<Integer> failedNodes = new ArrayList<>();

    public synchronized void add(Field field) {
        fieldsToAwake.add(field);
    }

    public synchronized void remove(Field field) {
        fieldsToAwake.remove(field);
    }

    public synchronized void nodeFailed(int failedNodeId) {
        failedNodes.add(failedNodeId); // mstodo translate to thread ids ?
        fieldsToAwake.forEach(f -> {
            synchronized (f) {
                f.notifyAll();
            }
        });
    }

    public synchronized void throwOnNodeFailure() { // mstodo would be good to do something to remove this synchronization
        if (!failedNodes.isEmpty()) {
            failedNodes.clear();
            throw new NodeFailedException(); // mstodo add info which node/threads failed
        }
    }
}
