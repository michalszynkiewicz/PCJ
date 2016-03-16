package gol;

import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.internal.faulttolerance.NodeFailedException;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

/**
 * N   NORTH border
 * ---------------------------------------------------------------------....
 * |        .        .        .        .       .        .       .       |
 * |        .        .        .        .       .        .       .       |    height
 * |....................................................................|....
 * |        .        .        .        .       .        .       .       |
 * M         |        .        .        .        .       .        .       .       |  EAST border
 * WEST      |....................................................................|
 * border    |        .        .        .        .       .        .       .       |
 * |        .        .        .        .       .        .       .       |
 * |....................................................................|
 * |        .        .        .        .       .        .       .       |
 * |        .        .        .        .       .        .       .       |
 * ----------------------------------------------------------------------
 * SOUTH border                     .       .
 * width
 * <p/>
 * <p/>
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/5/16
 * Time: 9:03 PM
 */
public class GameOfLifeFT2 extends Storage implements StartPoint {
    public static final int RIGHT = 0, LEFT = 1, BOTTOM = 2, TOP = 3;
    public static final int MAX_STEP = 100;
    public static final int BOTTOM_RIGHT = 0, BOTTOM_LEFT = 1, TOP_RIGHT = 2, TOP_LEFT = 3;

    //    public static final int MAX_STEP = 30;
    public static final String INCOMING_CORNERS = "incomingCorners";
    public static final String INCOMING_BORDERS = "incomingBorders";

    private int step = 0;
    private int prevStep = 1;
    private int stepParity = 0;

    private int board[][][];
    private int height, width;
    private int row, col; // position

    private File logFile;

    @Shared
    private int incomingBorders[][] = new int[4][];           // mstodo zastąpić barierę przez incoming borders [2][4][]
    private int incomingBordersWrk[][] = new int[4][];           // mstodo zastąpić barierę przez incoming borders [2][4][]
    @Shared
    private int incomingCorners[] = new int[]{0, 0, 0, 0};
    private int incomingCornersWrk[] = new int[]{0, 0, 0, 0};

    @Shared
    private Backup[] backups = new Backup[2];

    private void initBoard() {
        board = new int[2][height][width];
        if (col == 0) {
            createLightweightSpaceShip(10, 10);
        }
    }

    private void createLightweightSpaceShip(int iPos, int jPos) {
        System.out.println("initializing spaceship for step: " + stepParity);
        board[stepParity][iPos][jPos] = 1;
        board[stepParity][iPos][jPos + 3] = 1;
        board[stepParity][iPos + 1][jPos + 4] = 1;
        board[stepParity][iPos + 2][jPos + 4] = 1;
        board[stepParity][iPos + 3][jPos + 4] = 1;
        board[stepParity][iPos + 3][jPos + 3] = 1;
        board[stepParity][iPos + 3][jPos + 2] = 1;
        board[stepParity][iPos + 3][jPos + 1] = 1;
        board[stepParity][iPos + 2][jPos] = 1;
    }

    private void init() {
        try {
            logFile = File.createTempFile("pcj-gol-ft2", "log" + PCJ.myId());
        } catch (IOException e) {
            System.exit(0);
        }
        // mstodo change/calculate sizes
        height = 20;
        width = 20;
        row = 0;
        col = PCJ.myId();

        for (int i = 0; i < 2; i++) {
            incomingBordersWrk[i] = new int[height]; // mstodo differentiate widht/height based on row/col
        }
        for (int i = 2; i < 4; i++) {
            incomingBordersWrk[i] = new int[width]; // mstodo differentiate widht/height based on row/col
        }

        log("before initBoard");
        initBoard();
    }

    private void assertResult() {
        if (PCJ.myId() > 0) {
            assertAllZeroes();
        } else {
            assertLightweightSpaceShip(10, 10);
        }
        log("all assertions passed");
    }

    private void assertEquals(int board[][], int i, int j, int exp) {
        if (board[i][j] != exp) {
            log("invalid value. Expected: " + exp + ", found: " + board[i][j] + " on pos: " + i + ", " + j + "\n");
            System.exit(3);
        }
    }

    private void assertLightweightSpaceShip(int iPos, int jPos) { // mstodo assert the rest are zeroes
        int place = stepParity;
        assertAllZeroes();
        assertEquals(board[place], iPos, jPos, 1);
        assertEquals(board[place], iPos + 3, jPos, 1);
        assertEquals(board[place], iPos + 4, jPos + 1, 1);
        assertEquals(board[place], iPos + 4, jPos + 2, 1);
        assertEquals(board[place], iPos + 4, jPos + 3, 1);
        assertEquals(board[place], iPos + 3, jPos + 3, 1);
        assertEquals(board[place], iPos + 2, jPos + 3, 1);
        assertEquals(board[place], iPos + 1, jPos + 3, 1);
        assertEquals(board[place], iPos, jPos + 2, 1);
    }

    private void assertAllZeroes() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                assertEquals(board[stepParity], i, j, 0);
            }
        }
    }

    @Override
    public void main() throws Throwable {
        init();
        log("after init");

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
            log(out.toString());
            System.exit(123);
        }

        print();

    }

    private void log(String message) {
        try {
            FileWriter fw = new FileWriter(logFile, true);
            fw.append(message);

            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        log("\n\n@@ [" + step + "\n@@ " + PCJ.myId() + " --------------------------\n@@");
        for (int i = 10; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                log("" + (board[stepParity][i][j] == 1 ? '*' : ' '));
            }
            log("\n@@");
        }
        log("\n\n");
    }

//    private void print() {
//        int i0 = -1, j0 = -1;
//        OUTER:
//        for (int i = 0; i < height; i++) {
//            for (int j = 0; j < width; j++) {
//                if (board[stepParity][i][j] == 1) {
//                    i0 = i;
//                    System.out.print("i0 found for j: " + j);
//                    break OUTER;
//                }
//            }
//        }
//        OUTER:
//        for (int j = 0; j < width; j++) {
//            for (int i = 0; i < height; i++) {
//                if (board[stepParity][i][j] == 1) {
//                    j0 = j;
//                    System.out.println("\tj0 found for i: " + i);
//                    break OUTER;
//                }
//            }
//        }
//        if (i0 != -1 && j0 != -1) printFrom(i0, j0);
//    }
//
//
//    // prints 10x10 piece of the board chars
//    private void printFrom(int i0, int j0) {
//        log("\n\n[" + step + "]STARTING FROM: " + i0 + ", " + j0 + "\n@@ " +PCJ.myId() + " --------------------------\n@@");
//        for (int i = i0; i < i0 + 10 && i < 20; i++) {
//            for (int j = j0; j < j0 + 10 && j < 20; j++) {
//                log("" + (board[stepParity][i][j] == 1 ? '*' : ' '));
//            }
//            log("\n@@");
//        }
//        log("\n\n");
//    }

    private void share() {
        int[][] b = board[prevStep];
        share(nodeIdForRowAndColumn(row, col - 1), INCOMING_BORDERS, column(b, 0), RIGHT);
        share(nodeIdForRowAndColumn(row, col + 1), INCOMING_BORDERS, column(b, width - 1), LEFT);
        share(nodeIdForRowAndColumn(row - 1, col), INCOMING_BORDERS, b[0], BOTTOM);
        share(nodeIdForRowAndColumn(row + 1, col), INCOMING_BORDERS, b[height - 1], TOP);

        share(nodeIdForRowAndColumn(row - 1, col - 1), INCOMING_CORNERS, b[0][0], BOTTOM_RIGHT);
        share(nodeIdForRowAndColumn(row - 1, col + 1), INCOMING_CORNERS, b[0][width - 1], BOTTOM_LEFT);
        share(nodeIdForRowAndColumn(row + 1, col - 1), INCOMING_CORNERS, b[height - 1][0], TOP_RIGHT);
        share(nodeIdForRowAndColumn(row + 1, col + 1), INCOMING_CORNERS, b[height - 1][width - 1], TOP_LEFT);
    }

    private void waitForBorders() {
        log("[" + PCJ.myId() + "] mod count: " + modCount()+  " INCOMING BORDERS: \n");
        int incomingBordersCount = col == 0 || col == 3 ? 1 : 2;// mstodo
        PCJ.waitFor(INCOMING_BORDERS, incomingBordersCount);
        PCJ.waitFor("incomingCorners", 0);
        for (int i = 0; i < 4; i++) {
            log(" @" + i + "@ :" + Arrays.toString(incomingBorders[i]) + "\n");
        }
        for (int i=0; i<incomingBorders.length; i++) {
            if (incomingBorders[i] != null) {
                incomingBordersWrk[i] = incomingBorders[i].clone();
            }
        }
        incomingCornersWrk = incomingCorners.clone();
        PCJ.barrier();
    }

    private int modCount() {
        try {
            Field monitorFields = this.getClass().getSuperclass().getDeclaredField("monitorFields");
            monitorFields.setAccessible(true);
            Map<String, Integer> o = (Map<String, Integer>) monitorFields.get(this);
            return o.get(INCOMING_BORDERS);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private Integer nodeIdForRowAndColumn(int row, int col) {
        return row != 0 || col < 0 || col >= PCJ.threadCount() ? null : col; // mstodo the table is 4*1000 * 1000, for now assuming no failures
    }

    private int[] column(int[][] array, int idx) {
        int result[] = new int[height];
        for (int i = 0; i < height; i++) {
            result[i] = array[i][idx];
        }
        return result;
    }

    private void share(Integer nodeId, String variableName, Serializable value, int index) {
        int[] array = valueToArray(value);
        boolean nonZeros = false;
        if (Arrays.stream(array).anyMatch(i -> i != 0)) {
            log("[" + PCJ.myId() + "] COMING: " + Arrays.toString(array) + " for variable: " + variableName + " at pos: " + index);
            nonZeros = true;
        }
        if (nodeId != null) {
            PCJ.put(nodeId, variableName, value, index);
            if (nonZeros) log("shared\t INTO: " + nodeId);
        } else {
            if (nonZeros) log("skipped\n");
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
        int sum = sum(incomingBordersWrk[TOP], 0, 1) + sum(incomingBordersWrk[LEFT], 0, 1)
                + incomingCornersWrk[TOP_LEFT]
                + sum(prevBoard[1], 0, 1) + prevBoard[0][1];
        setCell(0, 0, sum);

        // TOP_RIGHT
        sum = sum(incomingBordersWrk[TOP], width - 2, width - 1) + sum(incomingBordersWrk[RIGHT], 0, 1)
                + incomingCornersWrk[TOP_RIGHT]
                + sum(prevBoard[1], width - 2, width - 1) + prevBoard[0][width - 2];
        setCell(0, width - 1, sum);

        // BOTTOM_LEFT
        sum = sum(incomingBordersWrk[BOTTOM], 0, 1) + sum(incomingBordersWrk[LEFT], 0, 1)
                + incomingCornersWrk[BOTTOM_LEFT]
                + sum(prevBoard[height - 2], 0, 1) + prevBoard[height - 1][1];
        setCell(height - 1, 0, sum);

        // BOTTOM_RIGHT
        sum = sum(incomingBordersWrk[BOTTOM], width - 2, width - 1) + sum(incomingBordersWrk[RIGHT], 0, 1)
                + incomingCornersWrk[BOTTOM_RIGHT]
                + sum(prevBoard[height - 2], width - 2, width - 1) + prevBoard[height - 1][width - 2];
        setCell(height - 1, width - 1, sum);
    }

    private void calculateBorders() {
        calcTopBorder();
        calcBottomBorder();
        setCellsWithOuterColumn(1, 0, incomingBordersWrk[LEFT]);
        setCellsWithOuterColumn(width - 2, width - 1, incomingBordersWrk[RIGHT]);
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
        setCellsForRows(height - 1, top, current, incomingBordersWrk[BOTTOM]);
    }

    private void calcTopBorder() {
        int[] current = board[prevStep()][0];
        int[] below = board[prevStep()][1];
        setCellsForRows(0, incomingBordersWrk[TOP], current, below);
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
            log("OBSERVED: " + liveNeighbors + "\t set value: " + value + "\n");
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
