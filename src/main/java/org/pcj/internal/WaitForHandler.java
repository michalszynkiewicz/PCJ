package org.pcj.internal;

import org.pcj.internal.faulttolerance.FaultTolerancePolicy;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 3/26/15
 * Time: 10:17 PM
 */
//mstodo analyze concurrency
    // mstodo put/get handling node failure?
public class WaitForHandler {

    private final FaultTolerancePolicy policy;
    private Set<Field> fieldsToAwake = new HashSet<>();

    public WaitForHandler(FaultTolerancePolicy policy) {
        this.policy = policy;
    }

    public void add(Field field) {
        synchronized (field) {
            fieldsToAwake.add(field);
        }
    }

    public void remove(Field field) {
        synchronized (field) {
            fieldsToAwake.remove(field);
        }
    }

    public void nodeFailed(int failedNodeId) {
        fieldsToAwake.forEach(f -> {
            synchronized (f) {
                f.notifyAll();
            }
        });
    }

    public void failOnNewFailure() {
        policy.failOnRemovedNode();
    }
}
