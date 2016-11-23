package test.gol;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/5/16
 * Time: 9:08 PM
 */
public class BoardSplitter {
    private final int M, N; // m rows, n columns

    private double communicationCost = 1000;
    private double checkpointPeriod = 100; // mstodo play with it


    public BoardSplitter(int m, int n) {
        M = m;
        N = n;
    }

    public Split split(int nodes) {
        Split bestSplit = calculateSplit(1, 1);
        for (int colNum = 1; colNum <= nodes; colNum ++) {
            for (int rowNum = 1; rowNum <= nodes/colNum; rowNum ++) {
                Split split = calculateSplit(colNum, rowNum);
                if (split.isBetterThan(bestSplit)) {
                    bestSplit = split;
                }
            }
        }
        return bestSplit;
    }

    private Split calculateSplit(int cols, int rows) {
        double height = Math.ceil((double)M/rows);
        double width = Math.ceil((double)N/cols);
        double calcCost = height * width;
        double phaseCommunicationCost = 2 * (height + width);
        double checkpointCost = height * width;
        double cost =  communicationCost * checkpointCost +
                (calcCost + communicationCost * phaseCommunicationCost) * checkpointPeriod;
        return new Split(cost, rows, cols, (int)height, (int)width);
    }

    public static class Split {
        public final int cols, rows;
        public final int height, width;
        private final double cost;

        public Split(double cost, int rows, int cols, int height, int width) {
            this.cost = cost;
            this.rows = rows;
            this.cols = cols;
            this.height = height;
            this.width = width;
        }

        public boolean isBetterThan(Split bestSplit) {
            return cost < bestSplit.cost;
        }
    }

}
