package org.pcj.internal.utils;

import java.io.File;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 10/13/16
 * Time: 1:37 PM
 */
public class FileUtils {
    public static void createParentDir(File file) {
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
    }
}
