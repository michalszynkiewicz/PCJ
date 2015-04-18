import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.Storage;

import java.lang.management.ManagementFactory;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 11/18/14
 * Time: 11:30 PM
 */
public class BarrierTest extends Storage implements StartPoint {
    @Override
    public void main() throws Throwable {
//         warmup
        for (int i = 0; i < 1000; i++) {
            PCJ.barrier();
        }
        long time = System.nanoTime();
        if (PCJ.myId() == 0) {
            System.out.println("START");
            PCJ.log("will sleep");
            Thread.sleep(8000l);
            PCJ.log("woken up");
        } else {
            PCJ.log("won't sleep");
        }
        for (int i = 0; i < 100; i++) {
            PCJ.barrier();
        }
//        PCJ.log(ManagementFactory.getRuntimeMXBean().getName());
//        PCJ.log("my thread number: " + PCJ.myId());
        if (PCJ.myId() == 0) {
            long nanos = System.nanoTime() - time;
            long millis = nanos/(1000*1000);
            long secs = millis/1000;
            millis -= secs*1000;
            nanos -= millis * 1000*1000;
            System.out.println("WORKING TIME: " + secs + "." + millis + "." + nanos + "ns");
        }
    }

    public static void main(String[] args) {
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());

        PCJ.deploy(BarrierTest.class, BarrierTest.class, System.getProperty("nodes").split(","));
//        new String[]{
//                "localhost",
//                "localhost"
//                "192.168.0.104",
//                "192.168.42.240",
//                "192.168.42.229"
//                "192.168.0.106",
//                "192.168.0.107",
//                "192.168.0.108"
//        });
    }
}
