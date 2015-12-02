/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing tree structure of nodes.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class CommunicationTree {

    private Integer root;
    private Integer parent;
    private Integer id; // mstodo
    private List<Integer> children;

    public CommunicationTree() {
        children = new ArrayList<>();
    }

    public Integer getRoot() {
        return root;
    }

    public void setRoot(Integer root) {
        this.root = root;
    }

    public Integer getParent() {
        return parent;
    }

    public void setParent(Integer parent) {
        this.parent = parent;
    }

    public List<Integer> getChildren() {
        return children;
    }

    public void setChildren(List<Integer> children) {
        this.children = children;
    }

    public void removeNode(int removedNodeIdx) {
        children.remove((Integer)removedNodeIdx);
    }

    @Override
    public String toString() {
        return "[id = " + id + ", parent = " + parent + ", root = " + root + " children: " + childrenToString();
    }

    private String childrenToString() {
        switch (children.size()) {
            case 0: return "NONE";
            case 1: return "left(" + children.get(0);
            case 2: return "left(" + children.get(0) + "), right(" + children.get(1) + ")";
            default: throw new IllegalStateException("invalid number of children: " + children.size());
        }
    }
}
