package test.gol.model;

import test.gol.Backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 8/6/16
 * Time: 11:55 PM
 */

public class Cut {
    final String sourceFile;
    final int srcX, srcY;
    final int targetX, targetY;
    final int w, h;
    final int data[][];


    public Cut(String sourceFile, int srcX, int srcY, int targetX, int targetY, int w, int h) {
        this.sourceFile = sourceFile;
        this.srcX = srcX;
        this.srcY = srcY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.w = w;
        this.h = h;
        data = new int[h][w]; // mstodo check w,h
    }

    public Void apply(String backupDir, int[][] board) {
        readData(backupDir);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                board[targetY + y][targetX + x] = data[y][x];
            }
        }
        return null;
    }

    public void readData(String backupDir) {
        Path path = Paths.get(backupDir, sourceFile);
        File source = path.toFile();
        try (InputStream stream = new FileInputStream(source);
             ObjectInputStream objectStream = new ObjectInputStream(stream)) {
            Backup b = (Backup) objectStream.readObject();
            for (int i = srcY; i < srcY + h; i++) {
                for (int j = srcX; j < srcX + w; j++) {
                    data[i - srcY][j - srcX] = b.board[i][j];
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException("Failed to read backup", e);
        }
    }

    private static boolean isBetween(int point, int left, int right) {
        return point >= left && point <= right;
    }

    public static Optional<Cut> getCut(Checkpoint checkpoint, int sourceNodeNum, int myAbsoluteX0, int myAbsoluteY0, int myW, int myH,
                                       int sourceAbsoluteX0, int sourceAbsoluteY0, int sourceW, int sourceH) {
        // mstodo check x/y <-> w/h !!!!
        Integer x = isBetween(myAbsoluteX0, sourceAbsoluteX0, sourceAbsoluteX0 + sourceW) ? myAbsoluteX0
                : (isBetween(sourceAbsoluteX0, myAbsoluteX0, myAbsoluteX0 + myW) ? sourceAbsoluteX0 : -1);
        Integer y = isBetween(myAbsoluteY0, sourceAbsoluteY0, sourceAbsoluteY0 + sourceH) ? myAbsoluteY0
                : (isBetween(sourceAbsoluteY0, myAbsoluteY0, myAbsoluteY0 + myH) ? sourceAbsoluteY0 : -1);

        if (x >= 0 && y >= 0) {
            int dx = Math.min(sourceAbsoluteX0 + sourceW, myAbsoluteX0 + myW) - x;
            int dy = Math.min(sourceAbsoluteY0 + sourceH, myAbsoluteY0 + myH) - y;

            return Optional.of(new Cut(checkpoint.toFileName(sourceNodeNum),
                    x - sourceAbsoluteX0, y - sourceAbsoluteY0,
                    x - myAbsoluteX0, y - myAbsoluteY0,
                    dx, dy));
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return "Cut{" +
                "sourceFile='" + sourceFile + '\'' +
                ", srcX=" + srcX +
                ", srcY=" + srcY +
                ", targetX=" + targetX +
                ", targetY=" + targetY +
                ", w=" + w +
                ", h=" + h +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
