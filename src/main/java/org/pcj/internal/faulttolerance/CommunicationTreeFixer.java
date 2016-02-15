package org.pcj.internal.faulttolerance;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.pcj.internal.faulttolerance.CommunicationNode.nullSafeId;
import static org.pcj.internal.faulttolerance.SetChild.Direction.LEFT;
import static org.pcj.internal.faulttolerance.SetChild.Direction.RIGHT;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 9/8/15
 * Time: 2:05 PM
 */
public class CommunicationTreeFixer {
    /**
     * method removes the tree and fixes it. It returns the list of updates applied on the tree in the process
     *
     * @param tree   tree to remove the node from
     * @param nodeId node identifier to remove
     * @return list of updates made to fix the tree
     */
    public static List<SetChild> remove(CommunicationNode tree, int nodeId) {
        CommunicationNode removed = tree.findNode(nodeId);
        CommunicationNode parent = removed.getParent();

        NodeWithDepth replacement = findDeepestLeafOnTheRightSideBut(tree, removed);
        CommunicationNode replacementNode = replacement.node;

        if (replacementNode == null) {
            return handleOnlyRootLeft(parent);
        }

        if (replacement.depth < removed.getDepth()) { // we're removing the deepest leaf
            return dettachDeepestLeaf(removed);
        }

        return handleDeepNodeRemoval(removed, parent, replacementNode);
    }

    private static List<SetChild> handleDeepNodeRemoval(CommunicationNode removed,
                                                        CommunicationNode parent,
                                                        CommunicationNode replacement) {
        List<SetChild> resultList = new ArrayList<>();
        CommunicationNode replacementParent = replacement.getParent();
        replaceWith(replacement, replacementParent, null, resultList);

        CommunicationNode left = removed.getLeft();
        CommunicationNode right = removed.getRight();

        replacement.setLeft(left);
        replacement.setRight(right);
        replacement.setParent(parent);

        // if a child of a removed node is null, there's no point to send update (replacement is a leaf originally, so there are nulls already attached to it)
        if (left != null) {
            resultList.add(new SetChild(replacement.getId(), left.getId(), LEFT));
        }
        if (right != null) {
            resultList.add(new SetChild(replacement.getId(), right.getId(), RIGHT));
        }
        replaceWith(removed, parent, replacement, resultList);

        return dropReplacementWithRemoved(resultList, removed);
    }

    private static List<SetChild> dropReplacementWithRemoved(List<SetChild> replacements, CommunicationNode removed) {
        List<SetChild> resultList = new ArrayList<>();

        for (SetChild replacement : replacements) {
            if (!replacement.touches(removed.getId())) {
                resultList.add(replacement);
            }
        }


        return resultList;
    }

    private static void replaceWith(CommunicationNode replaced, CommunicationNode parent, CommunicationNode replacement,
                                    List<SetChild> resultList) {
        if (parent.getLeft() == replaced) {
            parent.setLeft(replacement);
            resultList.add(
                    new SetChild(parent.getId(), nullSafeId(replacement), LEFT)
            );
        } else {
            parent.setRight(replacement);
            resultList.add(
                    new SetChild(parent.getId(), nullSafeId(replacement), RIGHT)
            );
        }
    }


    private static List<SetChild> dettachDeepestLeaf(CommunicationNode leaf) {
        CommunicationNode parent = leaf.getParent();
        parent.setLeft(null);
        parent.setRight(null);
        return asList(new SetChild(parent.getId(), null, LEFT),
                new SetChild(parent.getId(), null, RIGHT));
    }

    private static List<SetChild> handleOnlyRootLeft(CommunicationNode parent) {
        parent.setLeft(null);
        parent.setRight(null);
        return asList(new SetChild(parent.getId(), null, LEFT),
                new SetChild(parent.getId(), null, RIGHT));
    }

    private static NodeWithDepth findDeepestLeafOnTheRightSideBut(CommunicationNode tree, CommunicationNode excluded) {
        NodeWithDepth initialBest = new NodeWithDepth(null, -1);
        return findDeepestLeafOnTheRightSide(tree, 0, initialBest, excluded);
    }

    protected static NodeWithDepth findDeepestLeafOnTheRightSide(CommunicationNode node,
                                                                 int depth,
                                                                 NodeWithDepth currentBest,
                                                                 CommunicationNode excluded) {
        if (node == null) {
            return currentBest;
        } else if (node.isLeaf() && node.getId() != excluded.getId()) {
            if (currentBest.depth < depth) {
                return new NodeWithDepth(node, depth);
            }
        } else {
            currentBest = findDeepestLeafOnTheRightSide(node.getRight(), depth + 1, currentBest, excluded);
            return findDeepestLeafOnTheRightSide(node.getLeft(), depth + 1, currentBest, excluded);
        }
        return currentBest;
    }

    protected static class NodeWithDepth {
        protected CommunicationNode node;
        protected int depth;

        public NodeWithDepth(CommunicationNode node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }
}
