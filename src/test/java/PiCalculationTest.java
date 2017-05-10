import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.pcj.internal.faulttolerance.NodeFailedException;

import java.lang.management.ManagementFactory;
import java.util.Random;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/22/15
 * Time: 5:09 PM
 */
public class PiCalculationTest extends Storage implements StartPoint {
    private static final int fails = Integer.valueOf(System.getProperty("fails", "0"));
    public static final Long pointCount = Long.valueOf(System.getProperty("pointCount"));
    public static final Long FAIL_POINT = pointCount / 2;

    private static final Random random = new Random();
    private static final double radius = .5;
    private static final double x0 = .5, y0 = .5;
    public static final String COUNT = "count";

    private static boolean isInside(double x, double y) {
        return (x - x0) * (x - x0) + (y - y0) * (y - y0) <= radius * radius;
    }

    @Shared
    long count = 0;
    long localCount = 0;

    @Override
    public void main() throws Throwable {
        long time = System.nanoTime();
        long finish = pointCount;
        boolean fail = false;
        if (PCJ.getPhysicalNodeId() == 17 && fails > 1 && pointCount > FAIL_POINT) {
            finish = FAIL_POINT;
            fail = true;
        }
        if (PCJ.getPhysicalNodeId() == 2 && fails > 0 && pointCount > FAIL_POINT) {
            finish = FAIL_POINT;
            fail = true;
        }

        for (long i = 0; i < finish; i++) {
            double x = random.nextDouble(), y = random.nextDouble();
            if (isInside(x, y)) {
                localCount++;
            }
        }
        if (fail) {
            System.exit(0);
        }
        PCJ.putLocal("count", localCount);
        PCJ.barrier();

        if (PCJ.myId() == 0) {
            long totalPointCount = 0L;
            long totalMatchCount = 0L;
            for (int i = 0; i < PCJ.threadCount(); i++) {
                try {
                    totalMatchCount += PCJ.<Long>get(i, COUNT);
                    totalPointCount += pointCount;
                } catch (NodeFailedException nfe) {
                    nfe.printStackTrace(); //ignored - we want to continue calculations
                }
            }
            System.out.println("Pi = " + 4. * totalMatchCount / totalPointCount);
        }
        if (PCJ.myId() == 0) {
            long nanos = System.nanoTime() - time;
            long millis = nanos / (1000 * 1000);
            long secs = millis / 1000;
            millis -= secs * 1000;
            nanos -= millis * 1000 * 1000;
            System.out.println("[PiCalculationTest" + pointCount + "@" + PCJ.threadCount() + ", fails: " + fails + "] ####WORKING TIME: " + secs + "."
                    + String.format("%03d", millis) + "."
                    + String.format("%06d", nanos) + "ns");
        }
    }

    public static void main(String[] args) {
        String nodeProperty = System.getProperty("nodes");
        if (nodeProperty != null) {
            PCJ.deploy(PiCalculationTest.class, PiCalculationTest.class, nodeProperty.split(","));
        } else {
            if (args.length < 1) {
                System.err.println("Please pass file name for nodes file.");
                System.exit(13);
            }
            System.out.println(ManagementFactory.getRuntimeMXBean().getName());
            PCJ.start(PiCalculationTest.class, PiCalculationTest.class, args[0]);
        }
    }
}
