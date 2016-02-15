package org.pcj.internal.faulttolerance;

import org.junit.Test;
import org.pcj.internal.faulttolerance.CommunicationTreeFixer.NodeWithDepth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.StrictAssertions.fail;
import static org.pcj.internal.faulttolerance.CommunicationTreeFixer.findDeepestLeafOnTheRightSide;
import static org.pcj.internal.faulttolerance.SetChild.Direction.LEFT;
import static org.pcj.internal.faulttolerance.SetChild.Direction.RIGHT;


/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 9/9/15
 * Time: 9:09 AM
 */
public class CommunicationTreeFixerTest {

    @Test
    public void remove1From2NodeTree() {
        CommunicationNode root = createTree(2);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 1);
        assertThat(root.getLeft()).isNull();
        assertThat(root.getRight()).isNull();
        assertThat(updates).hasSize(2);

        assertThat(updates).containsAll(
                asList(
                        new SetChild(0, null, LEFT),
                        new SetChild(0, null, RIGHT)
                ));
    }

    @Test
    public void remove3From4NodeTree() {
        CommunicationNode root = createTree(4);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 3);
        assertThat(root.getLeft().getId()).isEqualTo(1);
        assertThat(root.getRight().getId()).isEqualTo(2);

        CommunicationNode two = root.findNode(1);
        assertThat(two.getLeft()).isNull();

        assertThat(updates).hasSize(2);

        assertThat(updates).containsAll(asList(
                new SetChild(1, null, LEFT),
                new SetChild(1, null, RIGHT)
        ));
    }

    @Test
    public void remove2From4NodeTree() {
        CommunicationNode root = createTree(4);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 2);
        assertThat(root.getLeft().getId()).isEqualTo(1);
        assertThat(root.getRight().getId()).isEqualTo(3);
        CommunicationNode two = root.findNode(1);
        assertThat(two.getRight()).isNull();
        assertThat(two.getLeft()).isNull();

        CommunicationNode four = root.findNode(3);
        assertThat(four.getParent().getId()).isEqualTo(0);

        assertThat(updates).hasSize(2);
        assertThat(updates).containsAll(asList(
                        new SetChild(1, null, LEFT),
                        new SetChild(0, 3, RIGHT)
                )
        );
    }

    @Test
    public void remove3From6NodeTree() {
        CommunicationNode root = createTree(6);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 3);
        assertThat(root.getLeft().getId()).isEqualTo(1);
        assertThat(root.getRight().getId()).isEqualTo(2); // nothing should change on root level

        CommunicationNode two = root.findNode(1);
        assertThat(two.getLeft().getId()).isEqualTo(5);
        assertThat(two.getRight().getId()).isEqualTo(4);

        CommunicationNode six = root.findNode(5);
        assertThat(six.getParent().getId()).isEqualTo(1);

        assertThat(updates).hasSize(2); // might be two with <6, null, null>
        assertThat(updates).containsAll(
                asList(
                        new SetChild(1, 5, LEFT), new SetChild(2, null, LEFT)
                ));
    }

    @Test
    public void remove2From8NodeTree() {
        CommunicationNode root = createTree(8);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 2);
        assertThat(root.getLeft().getId()).isEqualTo(1);
        assertThat(root.getRight().getId()).isEqualTo(7);

        CommunicationNode eight = root.findNode(7);
        assertThat(eight.getLeft().getId()).isEqualTo(5);
        assertThat(eight.getRight().getId()).isEqualTo(6);
        assertThat(eight.getParent().getId()).isEqualTo(0);

        assertThat(updates).hasSize(4);
        assertThat(updates).containsAll(asList(
                new SetChild(0, 7, RIGHT),
                new SetChild(3, null, LEFT),
                new SetChild(7, 5, LEFT),
                new SetChild(7, 6, RIGHT)
        ));
    }

    @Test
    public void remove2From7NodeTree() {
        CommunicationNode root = createTree(7);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 2);
        assertThat(root.getLeft().getId()).isEqualTo(1);
        assertThat(root.getRight().getId()).isEqualTo(6);

        CommunicationNode seven = root.findNode(6);
        assertThat(seven.getLeft().getId()).isEqualTo(5);
        assertThat(seven.getRight()).isNull();
        assertThat(seven.getParent().getId()).isEqualTo(0);

        assertThat(updates).hasSize(2);
        assertThat(updates).containsAll(asList(
                new SetChild(0, 6, RIGHT),
                new SetChild(6, 5, LEFT)
        ));
    }

    @Test
    public void remove5_6_9_10_1_2_7From20NodeTree() {
        CommunicationNode root = createTree(20);
        CommunicationTreeFixer.remove(root, 5);
        CommunicationTreeFixer.remove(root, 6);
        CommunicationTreeFixer.remove(root, 9);
        CommunicationTreeFixer.remove(root, 10);
        CommunicationTreeFixer.remove(root, 1);
        CommunicationTreeFixer.remove(root, 2);
        CommunicationTreeFixer.remove(root, 7);
        checkForDuplicates(root);
    }

    private void checkForDuplicates(CommunicationNode root) {
        List<Integer> nodes = gatherNodeIds(root);
        Collections.sort(nodes);
        System.out.println("Nodes: " +nodes);

        int prev = nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            if (prev == nodes.get(i)) {
                fail("Duplicate node found: " + prev);
            }
            prev = nodes.get(i);
        }
    }

    private List<Integer> gatherNodeIds(CommunicationNode root) {
        List<Integer> result = new ArrayList<>();
        result.add(root.getId());
        if (root.getLeft() != null) {
            result.addAll(gatherNodeIds(root.getLeft()));
        }
        if (root.getRight() != null) {
            result.addAll(gatherNodeIds(root.getRight()));
        }
        return result;
    }

    @Test
    public void remove2From20NodeTree() {
        CommunicationNode root = createTree(20);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 2);
        System.out.println(updates);    // mstodo remove or continue
    }

    @Test
    public void deepestLeafFor2NodesOmitting1() {
        CommunicationNode root = createTree(2);
        CommunicationNode two = root.findNode(1);

        NodeWithDepth result = findDeepestLeafOnTheRightSide(root, 0,
                new NodeWithDepth(null, -1), two);

        assertThat(result.node).isNull();
        assertThat(result.depth).isEqualTo(-1);
    }

    @Test
    public void deepestLeafFor6NodesOmitting5() {
        CommunicationNode root = createTree(6);
        CommunicationNode six = root.findNode(5);

        assertDeepestLeafOmitting(root, 4, 2, six);
    }

    @Test
    public void deepestLeafFor6NodesOmitting4() {
        CommunicationNode root = createTree(6);
        CommunicationNode four = root.findNode(4);

        assertDeepestLeafOmitting(root, 5, 2, four);
    }

    @Test
    public void deepestLeafFor2Nodes() throws Exception {
        CommunicationNode one = createTree(2);
        assertDeepestLeaf(one, 1, 1);
    }

    @Test
    public void deepestLeafFor3Nodes() throws Exception {
        CommunicationNode one = createTree(3);
        assertDeepestLeaf(one, 2, 1);
    }

    @Test
    public void deepestLeafFor4Omitting3() throws Exception {
        CommunicationNode one = createTree(4);
        assertDeepestLeafOmitting(one, 2, 1, one.findNode(3));
    }

    @Test
    public void deepestLeafFor4Nodes() throws Exception {
        CommunicationNode one = createTree(4);
        assertDeepestLeaf(one, 3, 2);
    }

    @Test
    public void deepestLeafFor6Nodes() throws Exception {
        CommunicationNode one = createTree(6);
        assertDeepestLeaf(one, 5, 2);
    }

    private CommunicationNode createTree(int noOfNodes) {
        return createSubtree(noOfNodes, 0, null);
    }


    private static void assertDeepestLeaf(CommunicationNode one, int expectedNodeNo, int expectedDepth) {
        CommunicationNode mock = new CommunicationNode(-2);
        assertDeepestLeafOmitting(one, expectedNodeNo, expectedDepth, mock);
    }

    private static void assertDeepestLeafOmitting(CommunicationNode one, int expectedNodeNo, int expectedDepth, CommunicationNode excluded) {
        NodeWithDepth result = findDeepestLeafOnTheRightSide(one, 0,
                new NodeWithDepth(null, -1), excluded);
        assertThat(result.node.getId()).isEqualTo(expectedNodeNo);
        assertThat(result.depth).isEqualTo(expectedDepth);
    }

    private CommunicationNode createSubtree(int noOfNodes, int nodeId, CommunicationNode parent) {
        if (nodeId >= noOfNodes) {
            return null;
        }
        CommunicationNode result = new CommunicationNode(nodeId);
        result.setParent(parent);
        result.setLeft(createSubtree(noOfNodes, 2 * nodeId + 1, result));
        result.setRight(createSubtree(noOfNodes, 2 * nodeId + 2, result));
        return result;
    }
}