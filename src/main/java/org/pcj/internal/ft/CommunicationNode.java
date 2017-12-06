package org.pcj.internal.ft;

import java.util.Map;

/**
 * In order to make Fault Tolerance extensions work, in explicit fixing the communication tree,
 * node 0 has to keep track of how the communication tree looks like.
 * <br>
 * This class represents a node in the structure that node 0 keeps.
 *
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 9/8/15
 * Time: 2:20 PM
 */
public class CommunicationNode {
    private CommunicationNode left, right, parent;
    private int id;

    public CommunicationNode(int id) {
        this.id = id;
    }

    public static CommunicationNode build(int start, int numOfNodes, CommunicationNode parent) {
        if (start >= numOfNodes) {
            return null;
        }
        CommunicationNode result = new CommunicationNode(start);
        result.left = build(2 * start + 1, numOfNodes, result);
        result.right = build(2 * start + 2, numOfNodes, result);
        result.parent = parent;

        System.out.printf("initialized tree for %d nodes\n", numOfNodes);
        return result;
    }

    public static void toNodeById(CommunicationNode tree, Map<Integer, CommunicationNode> resultMap) {
        if (tree != null) {
            resultMap.put(tree.id, tree);
            toNodeById(tree.left, resultMap);
            toNodeById(tree.right, resultMap);
        }
    }

    public int getId() {
        return id;
    }

    public CommunicationNode getParent() {
        return parent;
    }

    public CommunicationNode getRight() {
        return right;
    }

    public CommunicationNode getLeft() {
        return left;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    public void setLeft(CommunicationNode left) {
        this.left = left;
        if (left != null) {
            left.setParent(this);
        }
    }

    public void setRight(CommunicationNode right) {
        this.right = right;
        if (right != null) {
            right.setParent(this);
        }
    }

    public void setParent(CommunicationNode parent) {
        this.parent = parent;
    }

    public CommunicationNode findNode(int nodeId) {
        CommunicationNode result = id == nodeId ? this : null;
        result = findInIfNotFound(getLeft(), nodeId, result);
        result = findInIfNotFound(getRight(), nodeId, result);
        return result;
    }

    public int getDepth() {
        if (parent == null) {
            return 0;
        } else {
            return 1 + getParent().getDepth();
        }
    }

    private static CommunicationNode findInIfNotFound(CommunicationNode parent, int nodeId, CommunicationNode result) {
        if (result != null) {
            return result;
        }
        if (parent != null) {
            result = parent.findNode(nodeId);
        }
        return result;
    }

    @Override
    public String toString() {
        return "CommunicationNode{" +
                "left=" + nullSafeId(left) +
                ", right=" + nullSafeId(right) +
                ", parent=" + nullSafeId(parent) +
                ", id=" + id +
                '}';
    }

    public String treeAsString(String prefix) {
        return id +
                (left == null ? "" : ("\n" + prefix + "LEFT:" + left.treeAsString(prefix + "\t"))) +
                (right == null ? "" : ("\n" + prefix + "RIGHT:" + right.treeAsString(prefix + "\t")));
    }

    public static Integer nullSafeId(CommunicationNode node) {
        return node != null ? node.getId() : null;
    }

    public boolean contains(Integer failedPhysicalId) {
        return findNode(failedPhysicalId) != null;
    }
}