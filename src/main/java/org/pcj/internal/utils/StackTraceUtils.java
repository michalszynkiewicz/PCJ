package org.pcj.internal.utils;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/17/15
 * Time: 9:21 PM
 */
public class StackTraceUtils {
    public static void printPCJTraceToErr(Exception e) {
        System.err.print("PCJ part: ");
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            String className = stackTraceElement.getClassName();
            if (className.contains("pcj")) {
                System.err.print(stackTraceElement.toString());
            }
        }
        System.err.println("");
    }
}
