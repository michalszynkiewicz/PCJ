/*
 * Copyright (c) 2011-2017, PCJ Library
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.ft;

import java.io.Serializable;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/22/17
 */
public class SetChild implements Serializable {

    private int parent;

    private Integer child;
    private Direction direction;

    public boolean touches(int id) {
        return parent == id || (child != null && id == child);
    }

    public SetChild() {
    }

    public SetChild(int parent, Integer child, Direction direction) {
        this.parent = parent;
        this.child = child;
        this.direction = direction;
    }

    public Integer getParent() {
        return parent;
    }

    public Integer getChild() {
        return child;
    }

    public boolean isNewChild(Integer nodeId) {
        return nodeId.equals(child);
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SetChild that = (SetChild) o;

        if (parent != that.parent) return false;
        return !(child != null ? !child.equals(that.child) : that.child != null);

    }

    @Override
    public int hashCode() {
        int result = parent;
        result = 31 * result + (child != null ? child.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SetChild{" +
                "parent=" + parent +
                ", child=" + child +
                ", direction=" + direction +
                '}';
    }

    public enum Direction {
        LEFT, RIGHT
    }
}
