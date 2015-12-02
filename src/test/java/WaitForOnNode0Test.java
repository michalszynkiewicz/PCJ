import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.internal.faulttolerance.NodeFailedException;

import java.lang.management.ManagementFactory;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 3/30/15
 * Time: 11:25 PM
 */
public class WaitForOnNode0Test extends Storage implements StartPoint {

    @Shared
    private int a;

    @Override
    public void main() throws Throwable {
        System.out.println("starting");
        switch (PCJ.myId()) {
            case 0:
                try {
                    PCJ.waitFor("a");
                } catch (NodeFailedException e) {
                    System.out.println("error while waiting for first var change");
                    e.printStackTrace();
                }
                PCJ.waitFor("a");
                break;
            case 1:
                PCJ.log("will sleep");
                Thread.sleep(8000l);
                PCJ.put(0, "a", 1);
                PCJ.log("woken up");
                break;
            case 2:
                PCJ.log("will sleep");
                Thread.sleep(8000l);
                PCJ.put(0, "a", 2);
                PCJ.log("woken up");
                break;
        }

        PCJ.log("the winner is: " + a);

        PCJ.log(ManagementFactory.getRuntimeMXBean().getName());
        PCJ.log("my thread number: " + PCJ.myId());
    }

    public static void main(String... args) {
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());

        PCJ.deploy(WaitForOnNode0Test.class, WaitForOnNode0Test.class, System.getProperty("nodes").split(","));
    }
}
