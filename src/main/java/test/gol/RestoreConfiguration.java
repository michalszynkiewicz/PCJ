package test.gol;

import test.gol.model.Checkpoint;

import java.io.Serializable;
import java.util.Map;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 8/17/16
 * Time: 11:44 PM
 */

public class RestoreConfiguration implements Serializable {
    final Checkpoint checkpoint;
    final int newNodeCount;
    final Map<Integer, Integer> newNodeIdMap;

    public RestoreConfiguration(Checkpoint checkpoint, Map<Integer, Integer> newNodeIdMap) {
        this.checkpoint = checkpoint;
        this.newNodeIdMap = newNodeIdMap;
        newNodeCount = newNodeIdMap.size();
    }
}
