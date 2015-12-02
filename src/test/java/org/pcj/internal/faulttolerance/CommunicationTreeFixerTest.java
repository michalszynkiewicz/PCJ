package org.pcj.internal.faulttolerance;

import org.junit.Test;
import org.pcj.internal.faulttolerance.CommunicationTreeFixer.NodeWithDepth;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
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
    public void remove2From2NodeTree() {
        CommunicationNode root = createTree(2);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 2);
        assertThat(root.getLeft()).isNull();
        assertThat(root.getRight()).isNull();
        assertThat(updates).hasSize(2);

        assertThat(updates).containsAll(
                asList(
                        new SetChild(1, null, LEFT),
                        new SetChild(1, null, RIGHT)
                ));
    }

    @Test
    public void remove4From4NodeTree() {
        CommunicationNode root = createTree(4);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 4);
        assertThat(root.getLeft().getId()).isEqualTo(2);
        assertThat(root.getRight().getId()).isEqualTo(3);

        CommunicationNode two = root.findNode(2);
        assertThat(two.getLeft()).isNull();

        assertThat(updates).hasSize(2);

        assertThat(updates).containsAll(asList(
                new SetChild(2, null, LEFT),
                new SetChild(2, null, RIGHT)
        ));
    }

    @Test
    public void remove3From4NodeTree() {
        CommunicationNode root = createTree(4);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 3);
        assertThat(root.getLeft().getId()).isEqualTo(2);
        assertThat(root.getRight().getId()).isEqualTo(4);
        CommunicationNode two = root.findNode(2);
        assertThat(two.getRight()).isNull();
        assertThat(two.getLeft()).isNull();

        CommunicationNode four = root.findNode(4);
        assertThat(four.getParent().getId()).isEqualTo(1);

        assertThat(updates).hasSize(2);
        assertThat(updates).containsAll(asList(
                        new SetChild(2, null, LEFT),
                        new SetChild(1, 4, RIGHT)
                )
        );
    }

    @Test
    public void remove4From6NodeTree() {
        CommunicationNode root = createTree(6);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 4);
        assertThat(root.getLeft().getId()).isEqualTo(2);
        assertThat(root.getRight().getId()).isEqualTo(3); // nothing should change on root level

        CommunicationNode two = root.findNode(2);
        assertThat(two.getLeft().getId()).isEqualTo(6);
        assertThat(two.getRight().getId()).isEqualTo(5);

        CommunicationNode six = root.findNode(6);
        assertThat(six.getParent().getId()).isEqualTo(2);

        assertThat(updates).hasSize(2); // might be two with <6, null, null>
        assertThat(updates).containsAll(
                asList(
                        new SetChild(2, 6, LEFT), new SetChild(3, null, LEFT)
                ));
    }

    @Test
    public void remove3From8NodeTree() {
        CommunicationNode root = createTree(8);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 3);
        assertThat(root.getLeft().getId()).isEqualTo(2);
        assertThat(root.getRight().getId()).isEqualTo(8);

        CommunicationNode eight = root.findNode(8);
        assertThat(eight.getLeft().getId()).isEqualTo(6);
        assertThat(eight.getRight().getId()).isEqualTo(7);
        assertThat(eight.getParent().getId()).isEqualTo(1);

        assertThat(updates).hasSize(4);
        assertThat(updates).containsAll(asList(
                new SetChild(1, 8, RIGHT),
                new SetChild(4, null, LEFT),
                new SetChild(8, 6, LEFT),
                new SetChild(8, 7, RIGHT)
        ));
    }

    @Test
    public void remove3From7NodeTree() {
        CommunicationNode root = createTree(7);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 3);
        assertThat(root.getLeft().getId()).isEqualTo(2);
        assertThat(root.getRight().getId()).isEqualTo(7);

        CommunicationNode seven = root.findNode(7);
        assertThat(seven.getLeft().getId()).isEqualTo(6);
        assertThat(seven.getRight()).isNull();
        assertThat(seven.getParent().getId()).isEqualTo(1);

        assertThat(updates).hasSize(2);
        assertThat(updates).containsAll(asList(
                new SetChild(1, 7, RIGHT),
                new SetChild(7, 6, LEFT)
        ));
    }

    @Test
    public void remove2From20NodeTree() {
        CommunicationNode root = createTree(20);
        List<SetChild> updates = CommunicationTreeFixer.remove(root, 2);
        System.out.println(updates);    // mstodo remove or continue
    }

    @Test
    public void deepestLeafFor2NodesOmitting2() {
        CommunicationNode root = createTree(2);
        CommunicationNode two = root.findNode(2);

        NodeWithDepth result = findDeepestLeafOnTheRightSide(root, 0,
                new NodeWithDepth(null, -1), two);

        assertThat(result.node).isNull();
        assertThat(result.depth).isEqualTo(-1);
    }

    @Test
    public void deepestLeafFor6NodesOmitting6() {
        CommunicationNode root = createTree(6);
        CommunicationNode six = root.findNode(6);

        assertDeepestLeafOmitting(root, 5, 2, six);
    }

    @Test
    public void deepestLeafFor6NodesOmitting5() {
        CommunicationNode root = createTree(6);
        CommunicationNode five = root.findNode(5);

        assertDeepestLeafOmitting(root, 6, 2, five);
    }

    @Test
    public void deepestLeafFor2Nodes() throws Exception {
        CommunicationNode one = createTree(2);
        assertDeepestLeaf(one, 2, 1);
    }

    @Test
    public void deepestLeafFor3Nodes() throws Exception {
        CommunicationNode one = createTree(3);
        assertDeepestLeaf(one, 3, 1);
    }

    @Test
    public void deepestLeafFor4Omitting4() throws Exception {
        CommunicationNode one = createTree(4);
        assertDeepestLeafOmitting(one, 3, 1, one.findNode(4));
    }

    @Test
    public void deepestLeafFor4Nodes() throws Exception {
        CommunicationNode one = createTree(4);
        assertDeepestLeaf(one, 4, 2);
    }

    @Test
    public void deepestLeafFor6Nodes() throws Exception {
        CommunicationNode one = createTree(6);
        assertDeepestLeaf(one, 6, 2);
    }

    private CommunicationNode createTree(int noOfNodes) {
        return createSubtree(noOfNodes, 1, null);
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
        if (nodeId > noOfNodes) {
            return null;
        }
        CommunicationNode result = new CommunicationNode(nodeId);
        result.setParent(parent);
        result.setLeft(createSubtree(noOfNodes, 2 * nodeId, result));
        result.setRight(createSubtree(noOfNodes, 2 * nodeId + 1, result));
        return result;
    }
}