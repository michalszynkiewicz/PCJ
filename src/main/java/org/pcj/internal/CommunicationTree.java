package org.pcj.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class for representing part of communication tree.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class CommunicationTree {

    private final int rootNode;
    private int parentNode;
    private List<Integer> children;

    public CommunicationTree(int rootNode) {
        this.rootNode = rootNode;
        this.parentNode = -1;
        children = new CopyOnWriteArrayList<>();
    }

    public int getRootNode() {
        return rootNode;
    }

    public void setParentNode(int parentNode) {
        this.parentNode = parentNode;
    }

    public int getParentNode() {
        return parentNode;
    }

    public List<Integer> getChildrenNodes() {
        return children;
    }

    public void setChildren(List<Integer> children) {
        this.children = children;
    }

    public void removeNode(Integer removedNodeIdx) {
        children.remove(removedNodeIdx);
        System.out.println("removed" + removedNodeIdx);
    }
}
