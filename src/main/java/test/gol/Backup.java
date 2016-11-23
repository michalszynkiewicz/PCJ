package test.gol;

import java.io.Serializable;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 8/17/16
 * Time: 11:44 PM
 */
public class Backup implements Serializable {
    public final int board[][];
    public final int step, height, width, row, col;

    public Backup(int[][] board, int step, int height, int width, int row, int col) {
        this.board = board;
        this.step = step;
        this.height = height;
        this.width = width;
        this.row = row;
        this.col = col;
    }
}
