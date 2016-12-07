package org.pcj.internal.faulttolerance;

import org.pcj.internal.message.MessageNodeRemoved;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/17/15
 * Time: 12:01 AM
 */
public interface FaultTolerancePolicy {
    void handleNodeFailure(int physicalNodeId);

    void reportError(int nodeId, boolean waitForReconfiguration);

    void error(MessageNodeRemoved message);

    void register(Thread thread);

    void unregister(Thread thread);

    void failOnNewFailure();
}
