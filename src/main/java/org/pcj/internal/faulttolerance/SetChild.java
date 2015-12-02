package org.pcj.internal.faulttolerance;

import org.pcj.internal.network.MessageInputStream;
import org.pcj.internal.network.MessageOutputStream;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 9/8/15
 * Time: 8:26 AM
 */
public class SetChild {

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


    public void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(parent);
        bbos.writeInt(direction.ordinal());
        bbos.writeInt(child == null ? -1 : child);
    }

    public void readObjects(MessageInputStream bbis) {
        parent = bbis.readInt();
        direction = Direction.values()[bbis.readInt()];
        child = readNullableInt(bbis);
    }

    private static Integer readNullableInt(MessageInputStream bbis) {       // mstodo move somewhere
        int rawValue = bbis.readInt();
        return rawValue == -1 ? null : rawValue;
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
