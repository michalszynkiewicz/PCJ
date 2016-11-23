package org.pcj;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 3/22/16
 * Time: 7:28 AM
 */
public class CustomLog {
    private static StringBuffer buffer = new StringBuffer();

    static {
        System.out.println("initialized custom log");
    }

    public static void log(String message) {
        buffer.append(message);
    }

    public static void write(String fileName) {
        File f = new File(fileName);
        if (f.exists() && !f.delete()) {
            System.out.println("couldn't remove file: " + fileName);
        }
        try {
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            f.createNewFile();
            FileWriter fw = new FileWriter(f);
            fw.write(buffer.toString());
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void log(String message, Object value) {
        if (value.getClass().isArray() && int.class.isAssignableFrom(value.getClass().getComponentType())) {
            log(message + Arrays.toString((int[]) value));
        } else if (value instanceof Exception) {
            StringWriter out = new StringWriter();
            ((Exception)value).printStackTrace(new PrintWriter(out));
            log(message + out.toString());
        } else {
            log(message + value);
        }
    }
}
