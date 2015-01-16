package org.pcj.internal.failover;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/17/15
 * Time: 12:01 AM
 */
public interface FailoverPolicy {
    void handleNodeFailure(int physicalNodeId);
}
