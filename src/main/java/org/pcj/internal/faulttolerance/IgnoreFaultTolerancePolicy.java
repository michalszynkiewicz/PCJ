package org.pcj.internal.faulttolerance;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/25/15
 * Time: 10:43 PM
 */
public class IgnoreFaultTolerancePolicy implements FaultTolerancePolicy {
    @Override
    public void handleNodeFailure(int physicalNodeId) {
        System.out.println("\n\n\n\nnode failed\n\n");
        // mstodo: inform other nodes about failure
        // mstodo: remove node from all groups, maps etc
    }
}
