package test.gol;

import javafx.util.Pair;
import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.internal.faulttolerance.NodeFailedException;
import org.pcj.internal.utils.FileUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import test.gol.model.Checkpoint;
import test.gol.model.Cut;
import test.gol.utils.BackupUtils;
import test.gol.utils.UserThreadExecutorServicceFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static test.gol.utils.GameUtils.createLightweightSpaceShip;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/5/16
 * Time: 9:03 PM
 */
public class GameOfLifeFT2 extends Storage implements StartPoint {

    public static final String RESTORE_CONFIGURATION = "restoreConfiguration";
    public static final int MAX_RECOVERY_ATTEMPTS = 2;
    private final ExecutorService executor = UserThreadExecutorServicceFactory.newSingleThreadExecutor();

    public static final String RIGHT = "right", LEFT = "left", BOTTOM = "bottom", TOP = "top";
    public static final int MAX_STEP = 10000;
    public static final String BOTTOM_RIGHT = "bottomRight";
    public static final String BOTTOM_LEFT = "bottomLeft";
    public static final String TOP_RIGHT = "topRight";
    public static final String TOP_LEFT = "topLeft";

    private Path backupPath;

    private int step = 0;

    private int prevStep = 1;
    private int stepParity = 0;
    private int board[][][];

    private int noOfNodes; // mstodo set it!
    private int height, width;
    private int rowNum = 2, colNum = 2;
    private int row, col; // position
    private int myNum;

    private Map<Integer, Pair<Integer, Integer>> configurations;

    private final Set<Integer> failedThreads = new HashSet<>();

    @Shared
    private Boolean failed = false;

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

    //    @Shared
//    private Backup[] backups = new Backup[2];
    //    private int lastBackedUpStep;
//    private List<Integer> checkpointedThreads;
    private Path backupRoot = Paths.get("backup");
    @Shared
    private RestoreConfiguration restoreConfiguration;

    private void initBoard() {
        board = new int[2][height][width];
        createLightweightSpaceShip(board, stepParity, row, col, 10, 18);
    }

    private void init() {
        System.out.println("At node " + PCJ.getPhysicalNodeId());
        backupPath = backupRoot.resolve(String.valueOf(PCJ.myId())); // mstodo thread id ?
        // mstodo change/calculate sizes
        height = 20;
        width = 20;
        col = PCJ.myId() % 2;
        row = PCJ.myId() / 2;

        System.out.println("At node " + PCJ.getPhysicalNodeId() + " before for");

        for (int step = 0; step < 2; step++) {
            top[step] = new int[width];
            bottom[step] = new int[width];
            left[step] = new int[height];
            right[step] = new int[height];
        }

        System.out.println("At node " + PCJ.getPhysicalNodeId() + " after for");

        initBoard();
    }

    @Override
    public void main() throws Throwable {
        init();
        PCJ.barrier();

        try {
            while (step < MAX_STEP) {    // mstodo bring back
//            while (step < 3) {
                if (step == 1 && PCJ.myId() == 2) {
                    System.exit(13);
                }
                boolean recover = false;
                try {
                    if (checkpointTime()) {
                        asyncCheckpointToFile();
                    }
                    step();
                } catch (NodeFailedException e) {
                    System.out.println("NFE caught, will recover");
                    e.printStackTrace();
                    recover = true;
                }
                if (newNodesFailed() || recover) {
                    System.out.println("will recover");
                    recover();
                    System.out.println("recovered");
                }
            }
        } catch (Exception any) {
            any.printStackTrace();
            System.out.println("failed!");
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
        System.out.println("share");System.out.flush();
        int[][] b = board[prevStep];
        share(nodeIdForRowAndColumn(row, col - 1), RIGHT, column(b, 0), prevStep);
        share(nodeIdForRowAndColumn(row, col + 1), LEFT, column(b, width - 1), prevStep);
        share(nodeIdForRowAndColumn(row - 1, col), BOTTOM, b[0], prevStep);
        share(nodeIdForRowAndColumn(row + 1, col), TOP, b[height - 1], prevStep);

        share(nodeIdForRowAndColumn(row - 1, col - 1), BOTTOM_RIGHT, b[0][0], prevStep);
        share(nodeIdForRowAndColumn(row - 1, col + 1), BOTTOM_LEFT, b[0][width - 1], prevStep);
        share(nodeIdForRowAndColumn(row + 1, col - 1), TOP_RIGHT, b[height - 1][0], prevStep);
        share(nodeIdForRowAndColumn(row + 1, col + 1), TOP_LEFT, b[height - 1][width - 1], prevStep);
        System.out.println("shared will barrier");System.out.flush();
        PCJ.barrier(); // mstodo remove
        System.out.println("after barrier");System.out.flush();
    }

    private void waitForBorders() {
//        System.out.println("[" + PCJ.myId() + "] INCOMING BORDERS: \n");
        if (col > 0) {
//            System.out.println("for left");
            PCJ.waitFor(LEFT);
//            System.out.println("(" + System.nanoTime() + " @LEFT@ :" + Arrays.toString(left[prevStep]) + "\n");
        }
        if (col < colNum - 1) {
//            System.out.println("for right");
            PCJ.waitFor(RIGHT);
//            System.out.println("(" + System.nanoTime() + ") @RIGHT@ :" + Arrays.toString(right[prevStep]) + "\n");
        }
        if (row < rowNum - 1) {
//            System.out.println("for bottom");
            PCJ.waitFor(BOTTOM);
//            System.out.println("(" + System.nanoTime() + ") @BOTTOM@ :" + Arrays.toString(bottom[prevStep]) + "\n");
        }

        if (row > 0) {
//            System.out.println("for top");
            PCJ.waitFor(TOP);
//            System.out.println("(" + System.nanoTime() + ") @TOP@ :" + Arrays.toString(top[prevStep]) + "\n");
        }

        // CORNERS:

        if (col > 0 && row < rowNum - 1) {
//            System.out.println("for BOTTOM_LEFT");
            PCJ.waitFor(BOTTOM_LEFT);
//            System.out.println(" @BOTTOM_LEFT@ :" + topLeft[prevStep] + "\n");
        }
        if (col < colNum - 1 && row < rowNum - 1) {
//            System.out.println("for bottom right");
            PCJ.waitFor(BOTTOM_RIGHT);
//            System.out.println(" @RIGHT@ :" + bottomRight[prevStep] + "\n");
        }

        if (col > 0 && row > 0) {
//            System.out.println("for bottom left");
            PCJ.waitFor(TOP_LEFT);
//            System.out.println(" @TOP_LEFT@ :" + topLeft[prevStep] + "\n");
        }
        if (col < colNum - 1 && row > 0) {
//            System.out.println("for top right");
            PCJ.waitFor(TOP_RIGHT);
//            System.out.println(" @RIGHT@ :" + topRight[prevStep] + "\n");
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
//        System.out.println("\n(" + System.nanoTime() + " COMING -> " + nodeId + ": " + Arrays.toString(array) + " for variable: " + variableName + " at pos: " + Arrays.toString(indexes));
        if (nodeId != null) {
            PCJ.put(nodeId, variableName, value, indexes);
//            if (nonZeros) System.out.println("shared\t INTO: " + nodeId + "\n");
//        } else {
//            if (nonZeros) System.out.println("skipped\n");
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
            System.out.println("OBSERVED: " + liveNeighbors + "\t set value: " + value + "\n");
        }

        board[stepParity][i][j] = value;
    }

    private void recover() {
        System.out.println("Started recovery");
        int attemptsLeft = MAX_RECOVERY_ATTEMPTS;
        while (attemptsLeft > 0) {
            attemptsLeft --;
            Set<Integer> failedThreads = PCJ.getFailedThreadIds();
            try {
                doRecover();
                return;
            } catch (NodeFailedException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            if (failedThreads.containsAll(PCJ.getFailedThreadIds())) {
                return;
            }
        }
        if (attemptsLeft <= 0) {
            throw new RuntimeException("Reached maximum number of recoveries.");
        }
    }

    private void doRecover() throws IOException {
        System.out.println("Before barrier");
        System.out.flush();
        PCJ.barrier();
        System.out.println("After barrier");
        System.out.flush();
        if (PCJ.myId() == 0) {
            Optional<Entry<Checkpoint, List<String>>> latestBackup = findLatestBackup();
            System.out.println("will send out configuration ");
            System.out.flush();
            sendOutConfiguration(latestBackup);
            System.out.println("configuration send out");
            System.out.flush();
        } else {
            while (true) {
                try {
                    System.out.println("waiting for configuration");
                    System.out.flush();
                    PCJ.waitFor(RESTORE_CONFIGURATION);
                    System.out.println("got configuration: " + PCJ.getLocal(RESTORE_CONFIGURATION));
                    System.out.flush();
                    setRestoredBoard();
                    System.out.println("set restored board");
                    System.out.flush();
                    break;
                } catch (NodeFailedException ignored) {
                }
            }
        }
    }

    private void setRestoredBoard() {
        if (restoreConfiguration == null) {
            System.out.println("WARNING: no stored backup, will start calculations from the beginning.");
            init();
        } else {
            Checkpoint checkpoint = restoreConfiguration.checkpoint;
            myNum = restoreConfiguration.newNodeIdMap.get(PCJ.myId());
            int preNodeCount = checkpoint.nodeCount;
            // mstodo backup path?
            IntStream.range(0, preNodeCount).forEach(nodeNum -> {
                        Optional<Cut> cut = myCut(checkpoint, nodeNum);
                        cut.map(c -> c.apply(backupRoot.toAbsolutePath().toString(), board[step]));     // mstodo backupRoot may be passed earlier
                    }
            );
        }
    }

    private Optional<Cut> myCut(Checkpoint checkpoint, int sourceNodeNum) {
        int myAbsoluteX0 = absoluteX0(myNum, noOfNodes);
        int myAbsoluteY0 = absoluteY0(myNum, noOfNodes);
        int myW = w(myNum, noOfNodes), myH = h(myNum, noOfNodes);

        int sourceAbsoluteX0 = absoluteX0(sourceNodeNum, checkpoint.nodeCount);
        int sourceAbsoluteY0 = absoluteY0(sourceNodeNum, checkpoint.nodeCount);
        int sourceW = w(sourceNodeNum, checkpoint.nodeCount);
        int sourceH = h(sourceNodeNum, checkpoint.nodeCount);
        return Cut.getCut(checkpoint, sourceNodeNum, myAbsoluteX0, myAbsoluteY0, myW, myH, sourceAbsoluteX0, sourceAbsoluteY0, sourceW, sourceH);
    }

    private int h(int myNum, int noOfNodes) {
        throw new NotImplementedException();
    }

    private int w(int myNum, int noOfNodes) {
        throw new NotImplementedException();
    }

    private int absoluteY0(int myNum, int noOfNodes) {

        throw new NotImplementedException();
    }

    private int absoluteX0(int myNum, int noOfNodes) {
        Pair<Integer, Integer> wXh = configurations.get(noOfNodes);
        throw new NotImplementedException();
    }

    private void sendOutConfiguration(Optional<Entry<Checkpoint, List<String>>> latestBackup) {
        RestoreConfiguration configuration = latestBackup.map(e -> {
            Map<Integer, Integer> newIdMap = prepareNewIdMap();
            return new RestoreConfiguration(e.getKey(), newIdMap);
        }).orElse(new RestoreConfiguration(null, prepareNewIdMap()));

        PCJ.broadcast(RESTORE_CONFIGURATION, configuration);
    }

    private Map<Integer, Integer> prepareNewIdMap() {
        Map<Integer, Integer> resultMap = new HashMap<>();

        List<Integer> activeThreadIds = IntStream.range(0, PCJ.threadCount())
                .filter(i -> !PCJ.getFailedThreadIds().contains(i))
                .boxed()
                .collect(Collectors.toList());

        IntStream.range(0, activeThreadIds.size()).forEach(idx -> resultMap.put(idx, activeThreadIds.get(idx)));

        return resultMap;
    }

    private Optional<Entry<Checkpoint, List<String>>> findLatestBackup() throws IOException {
        Stream<String> fileNames = Files.list(backupPath)
                .map(p -> p.getFileName().toString());
        Map<Checkpoint, List<String>> backupFilesMap = fileNames
                .collect(groupingBy(Checkpoint::new));
        backupFilesMap = new TreeMap<>(backupFilesMap);

        return backupFilesMap
                .entrySet()
                .stream()
                .filter(e -> e.getKey().nodeCount == e.getValue().size())
                .findFirst();
    }

    private boolean checkpointTime() {

        return step % 1000 == 0; // mstodo dynamic?
    }

    private void asyncCheckpointToFile() throws IOException {
        int arr[][] = Stream.of(board[step % 2]).map(int[]::clone).toArray(int[][]::new);
        int step = this.step;
        int height = this.height;
        int width = this.width;
        int row = this.row;
        int col = this.col;

        System.out.println("Scheduled checkpoint for step: " + step);
        executor.submit(
                () -> {
                    try {
                        Path backupPath = this.backupPath.resolve(backupFilePath());
                        FileUtils.createParentDir(backupPath.toFile());
                        BackupUtils.createFile(backupPath);

                        System.out.println("checkpoint!");System.out.flush();
                        try (OutputStream stream = Files.newOutputStream(backupPath)) {
                            dumpBoard(arr, step, height, width, row, col, stream);
                        }
                        System.out.println("after checkpoint!");System.out.flush();
                    } catch (IOException e) {
                        PCJ.log("Error saving checkpoint: " + e.getMessage());
                        System.out.println("Error storing checkpoint");
                        e.printStackTrace();
                        PCJ.flushCustomLog("/tmp/gol/checkpoint-failure" + PCJ.myId());
                    }
                }
        );
    }

    private String backupFilePath() {
        return BackupUtils.backupFilePath(noOfNodes, step, myNum);
    }

    private void dumpBoard(int[][] arr, int step, int height, int width, int row, int col, OutputStream stream) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(stream)) {

            Backup backup = new Backup(arr, step, height, width, row, col);
            oos.writeObject(backup);
        }
    }

    private boolean newNodesFailed() {
        Set<Integer> newFailedThreads = PCJ.getFailedThreadIds();
        boolean result = failedThreads.addAll(newFailedThreads);
        System.out.println("new failures found: " + result);
        return result;
    }


    public static void main(String[] args) {
        PCJ.deploy(GameOfLifeFT2.class, GameOfLifeFT2.class,
                new String[]{
                        "127.0.0.1:8091",
                        "127.0.0.1:8191",
                        "127.0.0.1:8291",
                        "127.0.0.1:8391",
                });
    }
}
