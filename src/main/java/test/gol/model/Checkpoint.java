package test.gol.model;

import static test.gol.utils.BackupUtils.backupFilePath;

/**
 * mstodo: Header
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 8/7/16
 * Time: 12:08 AM
 */
public class Checkpoint implements Comparable<Checkpoint> {
    public final int nodeCount;
    public final int step;

    public Checkpoint(String path) {
        String[] split = path.split("_");
        this.nodeCount = Integer.valueOf(split[0]);
        this.step = Integer.valueOf(split[1]);
    }


    @Override
    public int compareTo(Checkpoint o) {
        int nodeDiff = Integer.compare(nodeCount, o.nodeCount);
        return nodeDiff == 0 ? Integer.compare(step, o.step) : nodeDiff;
    }

    public String toFileName(int nodeId) {
        return backupFilePath(nodeCount, step, nodeId);
    }
}
