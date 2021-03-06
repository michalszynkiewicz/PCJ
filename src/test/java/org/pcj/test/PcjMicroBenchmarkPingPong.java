package org.pcj.test;

/*
 * @author Piotr
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Locale;

import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

public class PcjMicroBenchmarkPingPong extends Storage implements StartPoint {

    @Shared
    double[] a;

    @Override
    public void main() {
        int[] transmit = {1, 10, 100, 1024, 2048, 4096, 8192, 16384,
            32768, 65536, 131072, 262144, 524288, 1048576, 2097152,
            4194304, 8388608, 16777216,};

        System.out.println("Maximum Heap Size: " + Runtime.getRuntime().maxMemory() + " B");

        final int ntimes = 100;
        final int number_of_tests = 5;
        double[] b;

        for (int j = 0; j < transmit.length; j++) {
            int n = transmit[j];

            PCJ.barrier();

            a = new double[n];
            b = new double[n];

            PCJ.barrier();
            PCJ.log(" s " + n + " " + a.length + " " + b.length);

            for (int i = 0; i < n; i++) {
                a[i] = (double) i + 1;
            }
            PCJ.barrier();

            //get 
            double tmin_get = Double.MAX_VALUE;
            for (int k = 0; k < number_of_tests; k++) {
                long time = System.nanoTime();
                for (int i = 0; i < ntimes; i++) {
                    if (PCJ.myId() == 0) {
                        b = PCJ.get(1, "a");
                    }
                }
                time = System.nanoTime() - time;
                double dtime = (time / (double) ntimes) * 1e-9;

                PCJ.log(PCJ.threadCount() + " get " + dtime + " " + b[n - 1]);
                PCJ.barrier();
                if (tmin_get > dtime) {
                    tmin_get = dtime;
                }
            }
            PCJ.barrier();

            // put  
            PCJ.monitor("a"); // dodane
            PCJ.barrier();
            for (int i = 0; i < n; i++) {
                a[i] = 0.0d;
                b[i] = (double) i + 1;
            }

            double tmin_put = Double.MAX_VALUE;
            for (int k = 0; k < number_of_tests; k++) {
                long time = System.nanoTime();
                for (int i = 0; i < ntimes; i++) {
                    if (PCJ.myId() == 0) {
                        PCJ.put(1, "a", b);
                    } else {
                        PCJ.waitFor("a");
                    }
                }

                time = System.nanoTime() - time;
                double dtime = (time / (double) ntimes) * 1e-9;

                PCJ.log(PCJ.threadCount() + " put " + dtime + " " + b[n - 1]);

                PCJ.barrier();
                if (tmin_put > dtime) {
                    tmin_put = dtime;
                }
            }

            // putB
            PCJ.monitor("a");
            PCJ.barrier();
            for (int i = 0; i < n; i++) {
                b[i] = (double) i + 1;
            }

            double tmin_putB = Double.MAX_VALUE;
            for (int k = 0; k < number_of_tests; k++) {
                long time = System.nanoTime();
                for (int i = 0; i < ntimes; i++) {
                    if (PCJ.myId() == i % 2) {
                        PCJ.put((i + 1) % 2, "a", b);
                    } else {
                        PCJ.waitFor("a");
                    }
                }

                time = System.nanoTime() - time;
                double dtime = (time / (double) ntimes) * 1e-9;

                PCJ.log(PCJ.threadCount() + " putB " + dtime + " " + b[n - 1]);

                PCJ.barrier();

                if (tmin_putB > dtime) {
                    tmin_putB = dtime;
                }
            }

            if (PCJ.myId() == 0) {
                System.out.format(Locale.FRANCE, "%5d size %10f \t t_get %7f \t t_put %7f \t t_putB %7f %n",
                        PCJ.threadCount(), (double) n / 128, tmin_get, tmin_put, tmin_putB);
            }

        }
    }

    public static void main(String[] args) {
        String[] nodesTxt = new String[1024];
        Scanner nf = null;
        try {
            nf = new Scanner(new File(args.length > 0 ? args[0] : "nodes.txt"));
        } catch (FileNotFoundException ex) {
            System.err.println("File not found!");
        }

        int n_nodes = 0;
        if (nf != null) {
            while (nf.hasNextLine()) {
                nodesTxt[n_nodes] = nf.nextLine();
                n_nodes++;
            }
        } else {
            for (int i = 0; i < 2; ++i) {
                nodesTxt[n_nodes] = "localhost:" + (8091 + i);
                n_nodes++;
            }
        }

        String[] nodes = new String[2];
        nodes[0] = nodesTxt[0];
        nodes[1] = nodesTxt[1];
        PCJ.deploy(PcjMicroBenchmarkPingPong.class, PcjMicroBenchmarkPingPong.class, nodes);
    }
}
