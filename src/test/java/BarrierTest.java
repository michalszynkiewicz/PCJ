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
public class BarrierTest extends Storage implements StartPoint {
    private static String[] nodes = getProperty("nodes").split(",");
    private static int fails = Integer.valueOf(getProperty("fails"));

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
//            if (i == 4998 && PCJ.getPhysicalNodeId() == 0) {
//                LogUtils.setEnabled(true);
//            }
//            LogUtils.log(PCJ.getPhysicalNodeId(), "-   will do barrier number: " + i);
            PCJ.barrier();
//            System.out.print(i + ",");
//            Lock.printLockState();
//            LogUtils.log(PCJ.getPhysicalNodeId(), "+   after barrier number: " + i);
            if (i == 200 && PCJ.getPhysicalNodeId() == 17 && fails > 1) {
                System.exit(12);
            }
            if (i == 100 && PCJ.getPhysicalNodeId() == 2 && fails > 0) {
                System.exit(12);
            }
        }
//        PCJ.log("After all barriers");
//        PCJ.log(ManagementFactory.getRuntimeMXBean().getName());
//        PCJ.log("my thread number: " + PCJ.myId());
        if (PCJ.myId() == 0) {
            long nanos = System.nanoTime() - time;
            long millis = nanos/(1000*1000);
            long secs = millis/1000;
            millis -= secs*1000;
            nanos -= millis * 1000*1000;
            System.out.println("[FaultTolerantBarrierTest@" + nodes.length + "] #### WORKING TIME: " + secs + "."
                    + String.format("%03d", millis) + "."
                    + String.format("%06d", nanos) + "ns");
        }
    }

    public static void main(String[] args) {
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        PCJ.deploy(BarrierTest.class, BarrierTest.class, nodes);
    }
}
