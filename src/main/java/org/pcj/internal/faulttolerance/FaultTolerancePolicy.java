package org.pcj.internal.faulttolerance;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/17/15
 * Time: 12:01 AM
 */
public interface FaultTolerancePolicy {
    void handleNodeFailure(int physicalNodeId);

    void reportError(int nodeId, boolean waitForReconfiguration);

    void onFailure(Runnable r);

    void onFailure(int nodeId, Runnable r);
}
