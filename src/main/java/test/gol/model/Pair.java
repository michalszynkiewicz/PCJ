package test.gol.model;

import java.io.Serializable;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 8/6/16
 * Time: 11:22 PM
 */
public class Pair<A1, A2> implements Serializable {
    public final A1 left;
    public final A2 right;

    public Pair(A1 left, A2 right) {
        this.left = left;
        this.right = right;
    }
}