import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.Storage;

import java.lang.management.ManagementFactory;

import static java.lang.System.getProperty;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 11/18/14
 * Time: 11:30 PM
 */
public class BarrierTestNonFT extends Storage implements StartPoint {

    @Override
    public void main() throws Throwable {
//         warmup
        for (int i = 0; i < 10000; i++) {
            PCJ.barrier();
        }
        long time = System.nanoTime();

        Integer barrierCount = Integer.valueOf(getProperty("barrierCount"));
        int batchSize = 1000;
        for (int i = 0; i < barrierCount; i++) {
            int count = Math.min(batchSize, barrierCount - i);
            for (int j=0; j<count; j++, i++) {
                PCJ.barrier();
            }
        }
        if (PCJ.myId() == 0) {
            long nanos = System.nanoTime() - time;
            long millis = nanos / (1000 * 1000);
            long secs = millis / 1000;
            millis -= secs * 1000;
            nanos -= millis * 1000 * 1000;
            System.out.println("[BarrierTestNonFT" + barrierCount + "@" + PCJ.threadCount() + "] #### WORKING TIME: " + secs + "."
                    + String.format("%03d", millis) + "."
                    + String.format("%06d", nanos) + "ns");
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please pass file name for nodes file.");
            System.exit(13);
        }
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        PCJ.start(BarrierTestNonFT.class, BarrierTestNonFT.class, args[0]);
    }
}
