package org.pcj.internal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/19/15
 * Time: 11:52 PM
 */
public class LogUtils {

    private static volatile boolean enabled = false;

    public static void setEnabled(boolean enabled) {
        LogUtils.enabled = enabled;
    }

    public static void log(int physicalId, String message){
        log("[" + physicalId + "] " + message);
    }

    public static void log(String message){
        if (enabled) {
            try {
                FileWriter fileWriter = new FileWriter("/tmp/pcj-ft-log", true);
                fileWriter.append(new Date().toString()).append("  ").append(message).append("\n");
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
