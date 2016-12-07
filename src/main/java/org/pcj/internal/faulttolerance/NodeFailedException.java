package org.pcj.internal.faulttolerance;

import java.util.Collection;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 2/1/15
 * Time: 10:30 PM
 */
public class NodeFailedException extends RuntimeException {
    public NodeFailedException() {}
    public NodeFailedException(Exception cause) {
        super(cause);
    }

    public NodeFailedException(int physicalNodeId) {
        super("Node failed: " + physicalNodeId);
    }
    public NodeFailedException(Collection<Integer> physicalNodeIds) {
        super("Nodes failed: " + physicalNodeIds);
    }
}
