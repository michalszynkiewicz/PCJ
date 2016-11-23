package test.gol.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 10/17/16
 * Time: 10:56 PM
 */
public class UserThreadExecutorServicceFactory {
    private static final ThreadFactory daemonThreadFactory = r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    };

    public static ExecutorService newSingleThreadExecutor() {
        return Executors.newSingleThreadExecutor(daemonThreadFactory);
    }
}
