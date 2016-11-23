package test.gol.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 8/7/16
 * Time: 12:10 AM
 */
public class BackupUtils {
    public static String backupFilePath(int no_of_nodes, int step, int id) {
        return "" + no_of_nodes + "_" + step + "_" + id;
    }

    public static void createFile(Path path) throws IOException {
        File file = path.toFile();
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
    }
}
