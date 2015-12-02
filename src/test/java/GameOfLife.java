import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.Storage;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 12/2/15
 * Time: 6:52 AM
 */
public class GameOfLife extends Storage implements StartPoint {
    private static final int N = 40;
    private static final int M = 40;
    private static final int MAX_STEP = 100; // mstodo change to sth smarter


    private int[][][] board = new int[2][N][M]; // mstodo change to boolean or short

    private void init() {
        // mstodo put some ones on the board[0]
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                board[0][i][j] = 0;
            }
        }
        board[0][1][1] = 1;
        board[0][2][1] = 1;
        board[0][1][2] = 1;
        board[0][2][2] = 1;

        board[0][6][5] = 1;
        board[0][6][6] = 1;
        board[0][6][7] = 1;
        board[0][5][7] = 1;
        board[0][4][6] = 1;
    }

    @Override
    public void main() throws Throwable {
        init();
        solve();
    }

    private void solve() {
        int stepIdx = 1;

        for (; stepIdx < MAX_STEP; stepIdx++) {
            int step = stepIdx % 2;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    int prevStep = (step + 1) % 2;
                    int liveNeighbors = sumOfNeighbors(prevStep, i, j);
                    if (board[prevStep][i][j] == 0) {
                        board[step][i][j] = liveNeighbors == 3 ? 1 : 0;
                    } else {
                        board[step][i][j] = (liveNeighbors == 2 || liveNeighbors == 3) ? 1 : 0;
                    }
                }
            }
            print(step);
        }
    }

    private void print(int step) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                System.out.print(board[step][i][j] == 0 ? ' ' : '*');
            }
            System.out.println();
        }

    }

    private int sumOfNeighbors(int step, int i, int j) {
        int sum = 0;
        for (int k = max(0, i - 1); k <= min(i + 1, N - 1); k++) {
            for (int l = max(0, j - 1); l <= min(j + 1, M - 1); l++) {
                if (k != i || l != j) {
                    sum += board[step][k][l];
                }
            }
        }
        return sum;
    }

    public static void main(String[] args) {
        PCJ.deploy(GameOfLife.class, GameOfLife.class,
                new String[]{
                        "localhost:8091",
                });
    }
}
