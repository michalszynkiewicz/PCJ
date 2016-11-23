package org.pcj.internal;

import org.pcj.internal.faulttolerance.CommunicationNode;
import org.pcj.internal.faulttolerance.CommunicationTreeFixer;
import org.pcj.internal.faulttolerance.SetChild;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/20/15
 * Time: 9:38 AM
 */
public class Node0Data {
    private CommunicationNode tree;
    private Map<Integer, CommunicationNode> nodesById = new HashMap<>();

    public Node0Data(int numOfNodes) {
        tree = CommunicationNode.build(0, numOfNodes, null);
        CommunicationNode.toNodeById(tree, nodesById);
    }

    public List<SetChild> remove(int node) {
        return CommunicationTreeFixer.remove(tree, node);
    }

    @Override
    public String toString() {
        return "Node0Data{" +
                "tree=" + tree +
                '}';
    }
}
