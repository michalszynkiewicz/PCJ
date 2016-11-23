package test.gol;

import java.io.Serializable;
import java.util.List;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/17/16
 * Time: 9:12 PM
 */
public class RecoveryData {
    int height, width, row, col, step;
    List<RecoveryPart> parts;

    public static class RecoveryPart implements Serializable {
        String fileName;
        int fileStartRow, fileStartCol, fileEndRow, fileEndCol;
        int startRow, startCol;
    }
}
