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
        if (PCJ.myId() != 0) {
            PCJ.log("will sleep");
            Thread.sleep(20000l);
            PCJ.log("woken up");
        } else {
            PCJ.log("won't sleep");
        }
        PCJ.barrier();
        PCJ.log(ManagementFactory.getRuntimeMXBean().getName());
        PCJ.log("my thread number: " + PCJ.myId());
    }

    public static void main(String[] args) {
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        PCJ.deploy(BarrierTest.class, BarrierTest.class, new String[]{
//                "localhost",
//                "localhost"
//                "192.168.0.104",
//                "192.168.42.240",
//                "192.168.42.229"
                "192.168.0.106",
                "192.168.0.107",
                "192.168.0.108"
        });
    }
}
