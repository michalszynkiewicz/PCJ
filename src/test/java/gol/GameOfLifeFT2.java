package gol;

import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.internal.faulttolerance.NodeFailedException;

import java.io.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * N   NORTH border
 * ---------------------------------------------------------------------....
 * |        .        .        .        .       .        .       .       |
 * |        .        .        .        .       .        .       .       |    height
 * |....................................................................|....
 * |        .        .        .        .       .        .       .       |
 * M  |        .        .        .        .       .        .       .       |  EAST border
 * WEST  |....................................................................|
 * border|        .        .        .        .       .        .       .       |
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
   public static final int BOTTOM_RIGHT = 0, BOTTOM_LEFT = 1, TOP_RIGHT = 2, TOP_LEFT = 3;

   public static final int MAX_STEP = 600;
   public static final String INCOMING_CORNERS = "incomingCorners";
   public static final String INCOMING_BORDERS = "incomingBorders";

   private int step = 0;

   private int board[][][];
   private int height, width;
   private int row, col; // position

   private File logFile;

   @Shared
   private int incomingBorders[][] = new int[4][];   // mstodo initialize all to zeroes, this way the ones that aren't coming are counted properly!
   @Shared
   private int incomingCorners[] = new int[]{0, 0, 0, 0};

   @Shared
   private Backup[] backups = new Backup[2];

   private void initBoard() {
      board = new int[2][height][width];
      if (col == 0) {
         createLightweightSpaceShip(10, 10);
      }
   }

   private void createLightweightSpaceShip(int iPos, int jPos) {
      board[step % 2][iPos][jPos] = 1;
      board[step % 2][iPos + 3][jPos] = 1;
      board[step % 2][iPos + 4][jPos + 1] = 1;
      board[step % 2][iPos + 4][jPos + 2] = 1;
      board[step % 2][iPos + 4][jPos + 3] = 1;
      board[step % 2][iPos + 4][jPos + 4] = 1;
      board[step % 2][iPos + 3][jPos + 4] = 1;
      board[step % 2][iPos + 2][jPos + 4] = 0; // mstodo rollback
      board[step % 2][iPos + 1][jPos + 3] = 1;
   }

   private void init() {
      try {
         logFile = File.createTempFile("pcj-gol-ft2", "log" + PCJ.myId());
      } catch (IOException e) {
         System.exit(0);
      }
      height = 100;
      width = 100;
      row = 0;
      col = PCJ.myId();

      for (int i = 0; i < 4; i++) {
         incomingBorders[i] = new int[width]; // mstodo differentiate widht/height based on row/col
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
         log("invalid value. Expected: " + exp + ", found: " + board[i][j] + " on pos: " + i + ", " + j);
         System.exit(3);
      }
   }

   private void assertLightweightSpaceShip(int iPos, int jPos) { // mstodo assert the rest are zeroes
      assertEquals(board[step % 2], iPos,jPos, 1);
      assertEquals(board[step % 2], iPos + 3,jPos, 1);
      assertEquals(board[step % 2], iPos + 4,jPos + 1, 1);
      assertEquals(board[step % 2], iPos + 4,jPos + 2, 1);
      assertEquals(board[step % 2], iPos + 4,jPos + 3, 1);
      assertEquals(board[step % 2], iPos + 4,jPos + 4, 1);
      assertEquals(board[step % 2], iPos + 3,jPos + 4, 1);
      assertEquals(board[step % 2], iPos + 2,jPos + 4, 1);
      assertEquals(board[step % 2], iPos + 1,jPos + 3, 1);
   }

   private void assertAllZeroes() {
      for (int i = 0; i < height; i++) {
         for (int j = 0; j < width; j++) {
            assertEquals(board[step % 2], i, j, 0);
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
               log("before step: " + step);
               step();
               log("after step: " + step);
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

      assertResult();
   }

   private void log(String message) {
      try {
         FileWriter fw = new FileWriter(logFile, true);
         fw.append("[" + PCJ.myId() + "]" + message).append("\n");

         fw.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private void step() {
      step++;
      share();
      calculateInternal();
      waitForBorders();
      calculateBorders();
      calculateCorners();

      print();
   }

   private void print() {

   }

   private void share() {
      int[][] b = board[prevStep()];
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
      int incomingBordersCount = col == 0 || col == 3 ? 1 : 2;// mstodo
      PCJ.waitFor(INCOMING_BORDERS, incomingBordersCount);
      PCJ.waitFor("incomingCorners", 0);
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
      if (nodeId != null) {
         log("will put to nodeId: " + nodeId);
         PCJ.put(nodeId, variableName, value, index);
      }
   }

   private void calculateInternal() {
      int prevStep = prevStep();
      for (int i = 1; i < height - 1; i++) {
         for (int j = 1; j < width - 1; j++) {
            setForNeighbors(prevStep, i, j);
         }
      }
   }

   private void calculateCorners() {
      // TOP_LEFT
      int prevStep = prevStep();
      int stepParity = this.step % 2;
      int[][] prevBoard = board[prevStep];
      int sum = sum(incomingBorders[TOP], 0, 1) + sum(incomingBorders[LEFT], 0, 1)
              + incomingCorners[TOP_LEFT]
              + sum(prevBoard[1], 0, 1) + prevBoard[0][1];
      setCell(0, 0, sum, stepParity, prevStep);

      // TOP_RIGHT
      sum = sum(incomingBorders[TOP], width - 2, width - 1) + sum(incomingBorders[RIGHT], 0, 1)
              + incomingCorners[TOP_RIGHT]
              + sum(prevBoard[1], width - 2, width - 1) + prevBoard[0][width - 2];
      setCell(0, width - 1, sum, stepParity, prevStep);

      // BOTTOM_LEFT
      sum = sum(incomingBorders[BOTTOM], 0, 1) + sum(incomingBorders[LEFT], 0, 1)
              + incomingCorners[BOTTOM_LEFT]
              + sum(prevBoard[height - 2], 0, 1) + prevBoard[height - 1][1];
      setCell(height - 1, 0, sum, stepParity, prevStep);

      // BOTTOM_RIGHT
      sum = sum(incomingBorders[BOTTOM], width - 2, width - 1) + sum(incomingBorders[RIGHT], 0, 1)
              + incomingCorners[BOTTOM_RIGHT]
              + sum(prevBoard[height - 2], width - 2, width - 1) + prevBoard[height - 1][width - 2];
      setCell(height - 1, width - 1, sum, stepParity, prevStep);
   }

   private void calculateBorders() {
      calcTopBorder();
      calcBottomBorder();
      setCellsWithOuterColumn(1, 0, incomingBorders[LEFT]);
      setCellsWithOuterColumn(width - 2, width - 1, incomingBorders[RIGHT]);
   }

   private void setCellsWithOuterColumn(int internalNeighborColIdx, int currentCol, int outerColumn[]) {
      int prevStep = prevStep();
      int prevBoard[][] = board[prevStep];
      for (int i = 1; i < height - 1; i++) {
         int sum = 0;
         sum += prevBoard[i - 1][internalNeighborColIdx];
         sum += prevBoard[i][internalNeighborColIdx];
         sum += prevBoard[i + 1][internalNeighborColIdx];
         sum += sum(outerColumn, i - 1, i + 1);
         sum += prevBoard[i - 1][currentCol];
         sum += prevBoard[i + 1][currentCol];
         setCell(i, currentCol, sum, step, prevStep);
      }
   }

   private void calcBottomBorder() {
      int[] top = board[prevStep()][height - 2]; // mstodo check if height >= 2, similar checks!
      int[] current = board[prevStep()][height - 1];
      setCellsForRows(top, current, incomingBorders[BOTTOM]);
   }

   private void calcTopBorder() {
      int[] current = board[prevStep()][0];
      int[] below = board[prevStep()][1];
      setCellsForRows(incomingBorders[TOP], current, below);
   }

   private void setForNeighbors(int prevStep, int i, int j) {
      int[] onTop = board[prevStep][i - 1];
      int[] current = board[prevStep][i];
      int[] below = board[prevStep][i + 1];
      setCellsForRows(onTop, current, below);
   }

   private void setCellsForRows(int[] top, int[] current, int[] bottom) {
      int prevStep = prevStep();
      for (int i = 1; i < width - 1; i++) {
         int sum = sum(top, i - 1, i + 1) + current[i - 1] + current[i + 1] + sum(bottom, i - 1, i + 1);
         setCell(0, i, sum, step, prevStep);
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

   private void setCell(int i, int j, int liveNeighbors, int step, int prevStep) {
      if (board[prevStep][i][j] == 0) {
         board[step % 2][i][j] = liveNeighbors == 3 ? 1 : 0;
      } else {
         board[step % 2][i][j] = (liveNeighbors == 2 || liveNeighbors == 3) ? 1 : 0;
      }
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
