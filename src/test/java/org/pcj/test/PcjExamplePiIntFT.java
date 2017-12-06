/*
 * Copyright (c) 2016, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.PcjFuture;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(PcjExamplePiIntFT.SharedEnum.class)
public class PcjExamplePiIntFT implements StartPoint {

    @Storage(PcjExamplePiIntFT.class)
    enum SharedEnum {
        sum
    }

    double sum;

    private double f(double x) {
        return (4.0 / (1.0 + x * x));
    }

    @Override
    public void main() throws Throwable {
        double pi = 0.0;
        long time = System.currentTimeMillis();
        if (PCJ.getNodeId() == 2) {
            System.out.println("killing!");
            System.exit(0);
        } else {
            System.out.println("not killing : " + PCJ.getNodeId());
        }
        for (int i = 1; i < 1000; ++i) {
            pi = calc(10000);                    // todo more points
        }
        time = System.currentTimeMillis() - time;
        if (PCJ.myId() == 0) {
            double err = pi - Math.PI;
            System.out.format("time %d\tsum = %7.5f, err = %10e\n", time, pi, err);
        }
    }

    private double calc(int N) {
        for (int i = PCJ.myId() + 1; i <= N; i += PCJ.threadCount()) {
            sum = sum + f((double) i - 0.5);
        }

        PcjFuture<Void> barrier = PCJ.asyncBarrier();
        if (PCJ.myId() == 0) {
            barrier.get();
            List<PcjFuture> data = PCJ.getThreadIds().stream()
                    .filter(threadId -> threadId != 0)
                    .map(threadId -> PCJ.asyncGet(threadId, SharedEnum.sum))
                    .collect(Collectors.toList());

            sum += data.stream().mapToDouble(future -> (double) future.get())
                    .sum();

            return sum / PCJ.threadCount();
        } else {
            return Double.NaN;
        }
    }

    public static void main(String[] args) {
        PCJ.deploy(PcjExamplePiIntFT.class,
                new NodesDescription(
                        new String[]{
                                "localhost:8091",
                                "localhost:8092",
                                "localhost:8092",
                                "localhost:8093",}));
    }
}
