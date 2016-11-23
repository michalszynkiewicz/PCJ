package test.gol.utils;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 8/17/16
 * Time: 11:03 PM
 */
public class GameUtils {

    public static void createLightweightSpaceShip(int[][][] board, int stepParity, int row, int col, int iPos, int jPos) {
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
}
