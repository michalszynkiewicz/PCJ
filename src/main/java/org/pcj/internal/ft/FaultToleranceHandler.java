/*
 * Copyright (c) 2011-2017, PCJ Library
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.ft;

import org.pcj.PCJ;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * TODO: To remove?
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 10/4/17
 */
public class FaultToleranceHandler {


    private FaultToleranceHandler() {
    }

    public void fail(Integer nodeId) {
        fail(singletonList(nodeId));
    }

    public void fail(int physicalNodeId, Exception cause) {
        fail(singletonList(physicalNodeId), cause);
    }

    public void fail(List<Integer> nodeIds) {
        List<Integer> threadIds = getThreadsForNodes(nodeIds);
        throw new NodeFailedException(nodeIds, threadIds);
    }

    public void fail(List<Integer> nodeIds, Exception cause) {
        List<Integer> threadIds = getThreadsForNodes(nodeIds);
        throw new NodeFailedException(nodeIds, threadIds, cause);
    }

    private List<Integer> getThreadsForNodes(List<Integer> nodeIds) {
        List<Integer> threadIds = new ArrayList<>();
        PCJ.getNodeData().getPhysicalIdByThreadId()
                .forEach((nodeId, threadId) -> {
                            if (nodeIds.contains(nodeId)) {
                                threadIds.add(threadId);
                            }
                        }
                );
        return threadIds;
    }


    private static FaultToleranceHandler faultToleranceHandler = new FaultToleranceHandler();

    public static FaultToleranceHandler get() {
        return faultToleranceHandler;
    }
}
