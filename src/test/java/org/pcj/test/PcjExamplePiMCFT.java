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
import org.pcj.internal.ft.NodeFailedException;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RegisterStorage(PcjExamplePiMCFT.SharedEnum.class)
public class PcjExamplePiMCFT implements StartPoint {

    @Storage(PcjExamplePiMCFT.class)
    enum SharedEnum {
        circleCount
    }

    long circleCount;

    @Override
    public void main() {
        if (PCJ.getNodeId() == 1) {
            System.exit(0);
        }
        Random random = new Random();
        long nAll = 512_000; // todo: rollback
        long n = nAll / PCJ.threadCount();

        double time = System.nanoTime();
// Calculate  
        for (long i = 0; i < n; ++i) {
            double x = 2.0 * random.nextDouble() - 1.0;
            double y = 2.0 * random.nextDouble() - 1.0;
            if ((x * x + y * y) < 1.0) {
                circleCount++;
            }
        }
        PCJ.barrier();
// Gather results 

        List<PcjFuture<Long>> cL = PCJ.getThreadIds().stream()
                .map(threadId -> PCJ.<Long>asyncGet(threadId, SharedEnum.circleCount))
                .collect(Collectors.toList());

        long c = 0L;
        int count = 0;
        for (PcjFuture<Long> future : cL) {
            try {
                c += future.get();
                count ++;
            } catch (NodeFailedException ignored) {}
        }

// Calculate pi 
        double pi = 4.0 * (double) c / (double) (n*count);
        time = System.nanoTime() - time;
// Print results         
        if (PCJ.myId() == 0) {
            System.out.println(pi + " " + time * 1.0E-9 + "s " + (pi - Math.PI));
        }
    }

    public static void main(String[] args) {

        PCJ.deploy(PcjExamplePiMCFT.class,
                new NodesDescription(
                        new String[]{"localhost:8081", "localhost:8082", "localhost:8083",
                                "localhost:8084", "localhost:8085"}
                )
        );
    }
}
