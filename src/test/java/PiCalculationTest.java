import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.internal.faulttolerance.NodeFailedException;

import java.lang.management.ManagementFactory;
import java.util.Random;

import static java.lang.System.getProperty;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/22/15
 * Time: 5:09 PM
 */
public class PiCalculationTest  extends Storage implements StartPoint {
    private static String[] nodes = getProperty("nodes").split(",");
    private static final int fails = Integer.valueOf(System.getProperty("fails", "0"));
    public static final Integer POINT_COUNT = Integer.valueOf(System.getProperty("pointCount"));
    public static final int FAIL_POINT = POINT_COUNT / 2;

    private static final Random random = new Random();
    private static final double radius = .5;
    private static final double x0 = .5, y0 = .5;
    public static final String COUNT = "count";

    private static boolean isInside(double x, double y) {
        return (x-x0)*(x-x0) + (y-y0)*(y-y0) <= radius * radius;
    }

    @Shared
    int count = 0;
    int localCount = 0;

    @Override
    public void main() throws Throwable {
        long time = System.nanoTime();
        for (int i = 0; i < POINT_COUNT; i++) {
            double x = random.nextDouble(), y = random.nextDouble();
            if (isInside(x, y)) {
                localCount ++;
            }
            if (i == FAIL_POINT && PCJ.getPhysicalNodeId() == 17 && fails > 1) {
                System.exit(12);
            }
            if (i == FAIL_POINT && PCJ.getPhysicalNodeId() == 2 && fails > 0) {
                System.exit(12);
            }
        }
        PCJ.putLocal("count", localCount);
        PCJ.barrier();

        if (PCJ.myId() == 0) {
            long totalPointCount = 0L;
            long totalMatchCount = 0L;
            for (int i = 0; i < PCJ.threadCount(); i++) {
                try {
                    totalMatchCount += PCJ.<Integer>get(i, COUNT);
                    totalPointCount += POINT_COUNT;
                } catch (NodeFailedException nfe) {
                    nfe.printStackTrace(); //ignored - we want to conitnue calculations
                }
            }
            System.out.println("Pi = " + 4. * totalMatchCount / totalPointCount);
        }
        if (PCJ.myId() == 0) {
            long nanos = System.nanoTime() - time;
            long millis = nanos/(1000*1000);
            long secs = millis/1000;
            millis -= secs*1000;
            nanos -= millis * 1000*1000;
            System.out.println("[PiCalculationTest@" + nodes.length + "] ####WORKING TIME: " + secs + "."
                    + String.format("%03d", millis) + "."
                    + String.format("%06d", nanos) + "ns");
        }
    }

    public static void main(String[] args) {
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        PCJ.deploy(PiCalculationTest.class, PiCalculationTest.class, nodes);
    }
}
