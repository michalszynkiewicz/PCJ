import org.pcj.*;

import java.util.Locale;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/29/15
 * Time: 11:39 PM
 */
public class NonFTPi extends Storage implements StartPoint {


    private double f(final double x) {
        return (4.0 / (1.0 + x * x));
    }

    @Shared
    double sum;

    @Override
    public void main() {
        PCJ.barrier();
        double time = System.nanoTime();

        for (int j = 0; j < 50; j++) {

//                      long nAll = 1280_000_000;
            long nAll = 100_000_000;

            double w = 1.0 / (double) nAll;
            sum = 0.0;

            for (int i = PCJ.myId(); i < nAll; i += PCJ.threadCount()) {
                sum = sum + f(((double) i + 0.5) * w);
            }
            sum = sum * w;
        }

        double time3 = System.nanoTime() - time;

        PCJ.barrier();

        double time2 = System.nanoTime();

        FutureObject cL[] = new FutureObject[PCJ.threadCount()];

        double pi = sum;
        if (PCJ.myId() == 0) {
            for (int p = 1; p < PCJ.threadCount(); p++) {
                cL[p] = PCJ.getFutureObject(p, "sum");
            }
            for (int p = 1; p < PCJ.threadCount(); p++) {
                pi = pi + (double) cL[p].get();
            }
        }

//        PCJ.log("c2+" +c);
        PCJ.barrier();

        time = System.nanoTime() - time;
        time2 = System.nanoTime() - time2;

        //      System.out.format(Locale.FRANCE, "%f7 time %f5   comm %f5 \n", pi, time * 1.0E-9, time2 * 1.0E-9);
        if (PCJ.myId() == 0) {
            System.out.format(Locale.FRANCE, "\n FF  %d  %f7 time %f5 calc %f5 comm %f5 \n", PCJ.threadCount(), pi, time * 1.0E-9, time3 * 1.0E-9, time2 * 1.0E-9);
            //      System.out.format(" %d  %f7 time %f5 \n", pi, time * 1.0E-9, time);
        }
    }

    public static void main(String[] args) {
        PCJ.deploy(NonFTPi.class, NonFTPi.class, System.getProperty("nodes").split(","));
    }
}
