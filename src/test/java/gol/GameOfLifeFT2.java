package gol;

import org.pcj.*;
import org.pcj.internal.faulttolerance.NodeFailedException;

import java.io.*;
import java.util.Arrays;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/5/16
 * Time: 9:03 PM
 */
public class GameOfLifeFT2 extends Storage implements StartPoint {
    public static final String RIGHT = "right", LEFT = "left", BOTTOM = "bottom", TOP = "top";
    public static final int MAX_STEP = 100;
    public static final String BOTTOM_RIGHT = "bottomRight";
    public static final String BOTTOM_LEFT = "bottomLeft";
    public static final String TOP_RIGHT = "topRight";
    public static final String TOP_LEFT = "topLeft";

    private int step = 0;
    private int prevStep = 1;
    private int stepParity = 0;

    private int board[][][];
    private int height, width;
    private int rowNum = 2, colNum = 2;
    private int row, col; // position

    @Shared
    private int top[][] = new int[2][];
    @Shared
    private int bottom[][] = new int[2][];
    @Shared
    private int right[][] = new int[2][];
    @Shared
    private int left[][] = new int[2][];

    @Shared
    private int[] bottomRight = new int[2];
    @Shared
    private int[] bottomLeft = new int[2];
    @Shared
    private int[] topRight = new int[2];
    @Shared
    private int[] topLeft = new int[2];

    @Shared
    private Backup[] backups = new Backup[2];

    private void initBoard() {
        board = new int[2][height][width];
        createLightweightSpaceShip(10, 18);
    }

    private void createLightweightSpaceShip(int iPos, int jPos) {
        System.out.println("initializing spaceship for step: " + stepParity);
        if (row > 0) {
            return;
        }

        if (col == 0) {
            board[stepParity][iPos][jPos] = 1;
            board[stepParity][iPos + 3][jPos] = 1;
            board[stepParity][iPos + 4][jPos + 1] = 1;
        }

        if (col == 1) {
            jPos = -2;
            board[stepParity][iPos + 4][jPos + 2] = 1;
            board[stepParity][iPos + 4][jPos + 3] = 1;
            board[stepParity][iPos + 3][jPos + 3] = 1;
            board[stepParity][iPos + 2][jPos + 3] = 1;
            board[stepParity][iPos + 1][jPos + 3] = 1;
            board[stepParity][iPos][jPos + 2] = 1;
        }
    }

    private void init() {
        // mstodo change/calculate sizes
        height = 20;
        width = 20;
        col = PCJ.myId() % 2;
        row = PCJ.myId() / 2;

        for (int step = 0; step < 2; step++) {
            top[step] = new int[width];
            bottom[step] = new int[width];
            left[step] = new int[height];
            right[step] = new int[height];
        }

//        PCJ.logCustom("before initBoard");
        initBoard();
    }

    @Override
    public void main() throws Throwable {
        init();
//        PCJ.logCustom("after init");

        try {
            while (step < MAX_STEP) {
                boolean recover = false;
                try {
                    if (checkpointTime()) {
                        checkpoint();
                    }
                    step();
                } catch (NodeFailedException e) {
                    recover = true;
                }
                if (newNodesFailed() || recover) {
                    recover();
                }
            }
        } catch (Exception any) {
            StringWriter out = new StringWriter();
            any.printStackTrace(new PrintWriter(out));
//            PCJ.logCustom(out.toString());
            System.exit(123);
        }

        print();
//        PCJ.flushCustomLog("/tmp/gol/log" + PCJ.myId());
    }

    private void step() {
        step++;
        stepParity = step % 2;
        prevStep = (step + 1) % 2;
        share();
        calculateInternal();
        waitForBorders();
        calculateBorders();
        calculateCorners();

        print();
    }

    private void print() {
        PCJ.logCustom("\n\n@@ [" + step + "\n@@ " + PCJ.myId() + " --------------------------\n@@");
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                PCJ.logCustom("" + (board[stepParity][i][j] == 1 ? '*' : ' '));
            }
            PCJ.logCustom("\n@@");
        }
        PCJ.logCustom("\n\n");
    }

    private void share() {
        int[][] b = board[prevStep];
        share(nodeIdForRowAndColumn(row, col - 1), RIGHT, column(b, 0), prevStep);
        share(nodeIdForRowAndColumn(row, col + 1), LEFT, column(b, width - 1), prevStep);
        share(nodeIdForRowAndColumn(row - 1, col), BOTTOM, b[0], prevStep);
        share(nodeIdForRowAndColumn(row + 1, col), TOP, b[height - 1], prevStep);

        share(nodeIdForRowAndColumn(row - 1, col - 1), BOTTOM_RIGHT, b[0][0], prevStep);
        share(nodeIdForRowAndColumn(row - 1, col + 1), BOTTOM_LEFT, b[0][width - 1], prevStep);
        share(nodeIdForRowAndColumn(row + 1, col - 1), TOP_RIGHT, b[height - 1][0], prevStep);
        share(nodeIdForRowAndColumn(row + 1, col + 1), TOP_LEFT, b[height - 1][width - 1], prevStep);
        PCJ.barrier(); // mstodo remove
    }

    private void waitForBorders() {
        PCJ.logCustom("[" + PCJ.myId() + "] INCOMING BORDERS: \n");
        if (col > 0) {
            PCJ.logCustom("for left");
            PCJ.waitFor(LEFT);
            PCJ.logCustom("(" + System.nanoTime() + " @LEFT@ :" + Arrays.toString(left[prevStep]) + "\n");
        }
        if (col < colNum - 1) {
            PCJ.logCustom("for right");
            PCJ.waitFor(RIGHT);
            PCJ.logCustom("(" + System.nanoTime() + ") @RIGHT@ :" + Arrays.toString(right[prevStep]) + "\n");
        }
        if (row < rowNum - 1) {
            PCJ.logCustom("for bottom");
            PCJ.waitFor(BOTTOM);
            PCJ.logCustom("(" + System.nanoTime() + ") @BOTTOM@ :" + Arrays.toString(bottom[prevStep]) + "\n");
        }

        if (row > 0) {
            PCJ.logCustom("for top");
            PCJ.waitFor(TOP);
            PCJ.logCustom("(" + System.nanoTime() + ") @TOP@ :" + Arrays.toString(top[prevStep]) + "\n");
        }

        // CORNERS:

        if (col > 0 && row < rowNum - 1) {
            PCJ.logCustom("for BOTTOM_LEFT");
            PCJ.waitFor(BOTTOM_LEFT);
            PCJ.logCustom(" @BOTTOM_LEFT@ :" + topLeft[prevStep] + "\n");
        }
        if (col < colNum - 1 && row < rowNum - 1) {
            PCJ.logCustom("for bottom right");
            PCJ.waitFor(BOTTOM_RIGHT);
            PCJ.logCustom(" @RIGHT@ :" + bottomRight[prevStep] + "\n");
        }

        if (col > 0 && row > 0) {
            PCJ.logCustom("for bottom left");
            PCJ.waitFor(TOP_LEFT);
            PCJ.logCustom(" @TOP_LEFT@ :" + topLeft[prevStep] + "\n");
        }
        if (col < colNum - 1 && row > 0) {
            PCJ.logCustom("for top right");
            PCJ.waitFor(TOP_RIGHT);
            PCJ.logCustom(" @RIGHT@ :" + topRight[prevStep] + "\n");
        }

    }

    private Integer nodeIdForRowAndColumn(int row, int col) {
        return (row < 0 || row >= rowNum || col < 0 || col >= colNum) ? null : row * colNum + (col % colNum);
    }

    private int[] column(int[][] array, int idx) {
        int result[] = new int[height];
        for (int i = 0; i < height; i++) {
            result[i] = array[i][idx];
        }
        return result;
    }

    private void share(Integer nodeId, String variableName, Serializable value, int... indexes) {
        int[] array = valueToArray(value);
        boolean nonZeros = false;
        if (Arrays.stream(array).anyMatch(i -> i != 0)) {
            nonZeros = true;
        }
        PCJ.logCustom("\n(" + System.nanoTime() + " COMING -> " + nodeId + ": " + Arrays.toString(array) + " for variable: " + variableName + " at pos: " + Arrays.toString(indexes));
        if (nodeId != null) {
            PCJ.put(nodeId, variableName, value, indexes);
            if (nonZeros) PCJ.logCustom("shared\t INTO: " + nodeId + "\n");
        } else {
            if (nonZeros) PCJ.logCustom("skipped\n");
        }
    }

    private int[] valueToArray(Serializable value) {
        if (value instanceof int[]) {
            return (int[]) value;
        } else {
            return new int[]{(Integer) value};
        }
    }


    private void calculateInternal() {
        for (int j = 1; j < height - 1; j++) {
            setForNeighbors(j);
        }
    }

    private void calculateCorners() {
        // TOP_LEFT
        int[][] prevBoard = board[prevStep];
        int sum = sum(top[prevStep], 0, 1) + sum(left[prevStep], 0, 1)
                + topLeft[prevStep]
                + sum(prevBoard[1], 0, 1) + prevBoard[0][1];
        setCell(0, 0, sum);

        // TOP_RIGHT
        sum = sum(top[prevStep], width - 2, width - 1) + sum(right[prevStep], 0, 1)
                + topRight[prevStep]
                + sum(prevBoard[1], width - 2, width - 1) + prevBoard[0][width - 2];
        setCell(0, width - 1, sum);

        // BOTTOM_LEFT
        sum = sum(bottom[prevStep], 0, 1) + sum(left[prevStep], height - 2, height - 1)
                + bottomLeft[prevStep]
                + sum(prevBoard[height - 2], 0, 1) + prevBoard[height - 1][1];
        setCell(height - 1, 0, sum);

        // BOTTOM_RIGHT
        sum = sum(bottom[prevStep], width - 2, width - 1) + sum(right[prevStep], height - 2, height - 1)
                + bottomRight[prevStep]
                + sum(prevBoard[height - 2], width - 2, width - 1) + prevBoard[height - 1][width - 2];
        setCell(height - 1, width - 1, sum);
    }

    private void calculateBorders() {
        calcTopBorder();
        calcBottomBorder();
        setCellsWithOuterColumn(1, 0, left[prevStep]);
        setCellsWithOuterColumn(width - 2, width - 1, right[prevStep]);
    }

    private void setCellsWithOuterColumn(int internalNeighborColIdx, int currentCol, int outerColumn[]) {
        int prevBoard[][] = board[prevStep];
        for (int i = 1; i < height - 1; i++) {
            int sum = 0;
            sum += prevBoard[i - 1][internalNeighborColIdx];
            sum += prevBoard[i][internalNeighborColIdx];
            sum += prevBoard[i + 1][internalNeighborColIdx];
            sum += sum(outerColumn, i - 1, i + 1);
            sum += prevBoard[i - 1][currentCol];
            sum += prevBoard[i + 1][currentCol];
            setCell(i, currentCol, sum);
        }
    }

    private void calcBottomBorder() {
        int[] top = board[prevStep()][height - 2]; // mstodo check if height >= 2, similar checks!
        int[] current = board[prevStep()][height - 1];
        setCellsForRows(height - 1, top, current, bottom[prevStep]);
    }

    private void calcTopBorder() {
        int[] current = board[prevStep][0];
        int[] below = board[prevStep][1];
        setCellsForRows(0, top[prevStep], current, below);
    }

    private void setForNeighbors(int i) {
        int[] onTop = board[prevStep][i - 1];
        int[] current = board[prevStep][i];
        int[] below = board[prevStep][i + 1];
        setCellsForRows(i, onTop, current, below);
    }

    private void setCellsForRows(int i, int[] top, int[] current, int[] bottom) {
        for (int j = 1; j < width - 1; j++) {
            int sum = sum(top, j - 1, j + 1) + current[j - 1] + current[j + 1] + sum(bottom, j - 1, j + 1);
            setCell(i, j, sum);
        }
    }

    private int sum(int array[], int from, int to) {
        int result = 0;
        for (int i = from; i <= to; i++) {
            result += array[i];
        }
        return result;
    }

    private int prevStep() {
        return (step + 1) % 2;
    }

    private void setCell(int i, int j, int liveNeighbors) {
        int value;

        if (board[prevStep][i][j] == 0) {
            value = liveNeighbors == 3 ? 1 : 0;
        } else {
            value = (liveNeighbors == 2 || liveNeighbors == 3) ? 1 : 0;
        }

        if (PCJ.myId() == 1 && i == 13 && j == 0) {
            PCJ.logCustom("OBSERVED: " + liveNeighbors + "\t set value: " + value + "\n");
        }

        board[stepParity][i][j] = value;
    }

    private void recover() {
        while (true) {
            try {
                doRecover();
                return;
            } catch (NodeFailedException ignored) {
            }
        }
    }

    private void doRecover() {

    }

    private boolean checkpointTime() {
        return false; // mstodo
    }

    private void checkpoint() {

    }

    private boolean newNodesFailed() {
        return false; // mstodo
    }


    public static void main(String[] args) {
        PCJ.deploy(GameOfLifeFT2.class, GameOfLifeFT2.class,
                new String[]{
                        "localhost:8091",
                        "localhost:8191",
                        "localhost:8291",
                        "localhost:8391",
                });
    }

    public static class Backup implements Serializable {
        int board[][];

    }
}
