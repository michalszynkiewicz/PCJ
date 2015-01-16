package org.pcj;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 10/22/14
 * Time: 10:29 PM
 */
public class NodeFailureException extends RuntimeException {
    private List<Integer> nodeIds;

    public NodeFailureException(Exception cause, List<Integer> nodeIds) {
        super(cause);
        this.nodeIds = nodeIds;
    }
    public NodeFailureException(Exception cause, Integer... nodeIds) {
        this(cause, asList(nodeIds));
    }

    public List<Integer> getNodeIds() {
        return nodeIds;
    }
}
