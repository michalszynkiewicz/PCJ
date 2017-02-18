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
//        if (PCJ.myId() == 0) {
//            System.out.println("START");
//            PCJ.log("will sleep");
//            Thread.sleep(8000l);
//            PCJ.log("woken up");
//        } else {
//            PCJ.log("won't sleep");
//        }
        Integer barrierCount = Integer.valueOf(getProperty("barrierCount"));
        for (int i = 0; i < barrierCount; i++) {
//            LogUtils.log(PCJ.getPhysicalNodeId(), "-   will do barrier number: " + i);
            PCJ.barrier();
//            LogUtils.log(PCJ.g    etPhysicalNodeId(), "+   after barrier number: " + i);
            if (i == 5000 && PCJ.getPhysicalNodeId() == 17) {
                // do nothing
            }
            if (i == 5000 && PCJ.getPhysicalNodeId() == 2) {
                // do nothing
            }
        }
//        PCJ.log("After all barriers");
//        PCJ.log(ManagementFactory.getRuntimeMXBean().getName());
//        PCJ.log("my thread number: " + PCJ.myId());
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
