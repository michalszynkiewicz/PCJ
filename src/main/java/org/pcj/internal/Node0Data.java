package org.pcj.internal;

import java.util.HashMap;
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
        tree = CommunicationNode.build(0, numOfNodes);
        CommunicationNode.toNodeById(tree, nodesById);
    }

    public CommunicationReplacement remove(int node) {
//        LogUtils.setEnabled(true);
        // LogUtils.log("will remove: " + node);
        CommunicationNode removed = nodesById.get(node);
        tree.remove(removed);
        CommunicationNode leaf = tree.findLeaf(node);
        leaf.left = removed.left;
        leaf.right = removed.right;
        Integer newLeftChild = nullSafeId(leaf.left);
        Integer newRightChild = nullSafeId(leaf.right);

        // LogUtils.log("found replacement: " + result);
        return new CommunicationReplacement(leaf.id, newLeftChild, newRightChild);
    }

    private Integer nullSafeId(CommunicationNode node) {
        return node == null ? null : node.id;
    }

    private static class CommunicationNode {
        private CommunicationNode left, right;
        private int id;

        public CommunicationNode(int id) {
            this.id = id;
        }

        public CommunicationNode findLeaf(int node) {
            if (isNullOrOmitted(left, node) && isNullOrOmitted(right, node)) {
                return this;
            } else {
                if (!isNullOrOmitted(left, node)) {
                    return left.findLeaf(node);
                } else {
                    return right.findLeaf(node);
                }
            }
        }

        boolean isNullOrOmitted(CommunicationNode node, int omitted) {
            return node == null || node.id == omitted;
        }

        public static CommunicationNode build(int start, int numOfNodes) {
            if (start >= numOfNodes) {
                return null;
            }
            CommunicationNode result = new CommunicationNode(start);
            result.left = build(2 * start + 1, numOfNodes);
            result.right = build(2 * start + 2, numOfNodes);
            return result;
        }

        public static void toNodeById(CommunicationNode tree, Map<Integer, CommunicationNode> resultMap) {
            if (tree != null) {
                resultMap.put(tree.id, tree);
                toNodeById(tree.left, resultMap);
                toNodeById(tree.right, resultMap);
            }
        }

        public CommunicationNode findParent(CommunicationNode node) { // todo speed up
            if (left == node || right == node) {
                return this;
            }

            CommunicationNode parent = null;
            if (left != null) {
                parent = left.findParent(node);
            }
            if (parent == null && right != null) {
                parent = right.findParent(node);
            }
            return parent;
        }

        public void remove(CommunicationNode removed) {
            CommunicationNode parent = findParent(removed);
            if (parent.left == removed) {
                parent.left = null;
            } else if (parent.right == removed) {
                parent.right = null;
            }
        }
    }

    public static class CommunicationReplacement {
        public int parent;
        public Integer newLeftChild;
        public Integer newRightChild;

        public CommunicationReplacement(int parent, Integer newLeftChild, Integer newRightChild) {
            this.parent = parent;
            this.newLeftChild = newLeftChild;
            this.newRightChild = newRightChild;
        }

        public boolean doesReplace() {
            return newLeftChild != null || newRightChild != null;
        }

        @Override
        public String toString() {
            return "CommunicationReplacement{" +
                    "parent=" + parent +
                    ", newLeftChild=" + newLeftChild +
                    ", newRightChild=" + newRightChild +
                    '}';
        }
    }
}
