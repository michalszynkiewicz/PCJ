//package gol;
//
//import gol.BoardSplitter.Split;
//import org.pcj.PCJ;
//import org.pcj.Shared;
//import org.pcj.StartPoint;
//import org.pcj.Storage;
//import org.pcj.internal.faulttolerance.NodeFailedException;
//
//import java.util.HashSet;
//import java.util.Set;
//
//import static java.lang.Integer.valueOf;
//import static java.lang.Math.max;
//import static java.lang.Math.min;
//import static java.lang.System.getProperty;
//
///**
// *                                N   NORTH border
// *      ---------------------------------------------------------------------....
// *      |        .        .        .        .       .        .       .       |
// *      |        .        .        .        .       .        .       .       |    height
// *      |....................................................................|....
// *      |        .        .        .        .       .        .       .       |
// *   M  |        .        .        .        .       .        .       .       |  EAST border
// *WEST  |....................................................................|
// *border|        .        .        .        .       .        .       .       |
// *      |        .        .        .        .       .        .       .       |
// *      |....................................................................|
// *      |        .        .        .        .       .        .       .       |
// *      |        .        .        .        .       .        .       .       |
// *      ----------------------------------------------------------------------
// *                          SOUTH border                     .       .
// *                                                            width
// *
// *
// * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
// * Date: 1/5/16
// * Time: 9:03 PM
// */
//public class GameOfLifeFT extends Storage implements StartPoint {
//
//    private static final int M = valueOf(getProperty("board.M", "1000"));
//    private static final int N = valueOf(getProperty("board.N", "1000"));
//    private static final int steps = valueOf(getProperty("steps", "1000"));
//    private static final int checkpointPeriod = valueOf(getProperty("checkpointPeriod", "1000"));
//    private static final BoardSplitter splitter = new BoardSplitter(M, N);
//    private static final int NORTH = 0;
//    private static final int SOUTH = 1;
//    private static final int EAST = 2;
//    private static final int WEST = 3;
//
//    @Shared
//    private CheckpointData checkpointData[];
//    @Shared
//    private int south[];
//    @Shared
//    private int north[];
//    @Shared
//    private int east[];
//    @Shared
//    private int west[];
//
//
//    private int stepIdx = 0;
//    private int board[][][];  // step x M x N
//    private final Set<Integer> failedNodes = new HashSet<>();
//
//    private Split split; // mstodo fill it!!!
//
//    @Override
//    public void main() throws Throwable {
//        checkpoint(); // if this fails, we quit
//        int stepIdx = 0;
//
//        while (stepIdx < steps) {
//            if (newNodesFailed()) {
//                safe(this::restoreCheckpoint);
//            } else {                              // mstodo check if failure between checkpoints doesn't screw this up
//                if (isCheckpointTime()) {
//                    safe(this::checkpoint);
//                }
//                step();
//            }
//        }
//    }
//
//    private void safe(Runnable call) {
//        while (true) {
//            try {
//                call.run();
//                break;
//            } catch (NodeFailedException ignored) {}
//        }
//    }
//
//    private void step() {             // mstodo first calc internal, then wait for borders and calc borders
//        int step = stepIdx % 2;
//        shareBorders((step + 1) % 2);
//        calcInternal(step);
//        waitForBorders();
//        calcBorders(step);
//        stepIdx++;
//    }
//
//    private void calcInternal(int step) {
//        for (int i = 1; i < N - 1; i++) {
//            for (int j = 1; j < M - 1; j++) {
//                int prevStep = (step + 1) % 2;
//                int liveNeighbors = sumOfNeighbors(prevStep, i, j);
//                setCell(step, i, j, liveNeighbors);
//            }
//        }
//    }
//
//    private void setCell(int step, int i, int j, int liveNeighbors) {
//        if (i == 0) {
//            board[step][i][j] = liveNeighbors == 3 ? 1 : 0;
//        } else {
//            board[step][i][j] = (liveNeighbors == 2 || liveNeighbors == 3) ? 1 : 0;
//        }
//    }
//
//    private void waitForBorders() {
//        PCJ.waitFor("boarders", split.numberOfNeighbors());
//        PCJ.waitFor("corners", split.numberOfCorners());
//    }
//
//    private void calcBorders(int step) {
//        for (int i = 0; i < N; i++) { // north
//            board[step][0][i] =
//        }
//    }
//
//    private void shareBorders(int stepToShare) {
//        shareBorder(stepToShare, split.north, SOUTH); // mstodo a moethod for north, south, etc
//        shareBorder(stepToShare, split.south, NORTH);
//        shareBorder(stepToShare, split.west, EAST);
//        shareBorder(stepToShare, split.east, WEST);
//        shareAngle(stepToShare, split.northeast, NORTH_EAST); // mstodo point sharing
//        shareAngle(stepToShare, split.northeast, NORTH_EAST); // mstodo point sharing
//        shareAngle(stepToShare, split.northeast, NORTH_EAST); // mstodo point sharing
//        shareAngle(stepToShare, split.northeast, NORTH_EAST); // mstodo point sharing
//    }
//
//
//
//    private int sumOfNeighbors(int step, int i, int j) {
//        int sum = 0;
//        // mstodo borders support
//        for (int k = max(0, i - 1); k <= min(i + 1, N - 1); k++) {
//            for (int l = max(0, j - 1); l <= min(j + 1, M - 1); l++) {
//                if (k != i || l != j) {
//                    sum += board[step][k][l];
//                }
//            }
//        }
//        return sum;
//    }
//
//    private boolean newNodesFailed() {
//        return failedNodes.size() < PCJ.getFailedThreadIds().size();
//    }
//
//    private boolean isCheckpointTime() {
//        return stepIdx % checkpointPeriod == 0;
//    }
//
//
//    private void restoreCheckpoint() {
//        PCJ.waitFor("checkpointRestoreData");
//        checkPointRestoreData
//        // mstodo
//        failedNodes.addAll(PCJ.getFailedThreadIds());
//    }
//
//
//    private void checkpoint() {
//        int failedNodesBefore = PCJ.getFailedThreadIds().size();
//        PCJ.put(backupNodeId(), "backup", prepareBackupData(), (stepIdx / checkpointPeriod) % 2);
//        PCJ.barrier();
//        int failedNodesAfter = PCJ.getFailedThreadIds().size();
//        // mstodo all the nodes made the backup for the same amount of nodes
//    }
//
//    public static void main(String[] args) {
//        PCJ.deploy(GameOfLife.class, GameOfLife.class,
//                new String[]{
//                        "localhost:8090",
//                        "localhost:8190",
//                        "localhost:8290",
//                        "localhost:8091",
//                        "localhost:8191",
//                        "localhost:8291",
//                        "localhost:8092",
//                        "localhost:8192",
//                        "localhost:8292",
//                });
//    }
//}
