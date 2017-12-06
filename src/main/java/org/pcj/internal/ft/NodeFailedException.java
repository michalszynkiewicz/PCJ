/*
 * Copyright (c) 2011-2017, PCJ Library
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.ft;

import java.util.Collection;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/3/17
 */
public class NodeFailedException extends RuntimeException {
    private final Collection<Integer> nodeIds;
    private final Collection<Integer> threadIds;

    public NodeFailedException(Collection<Integer> nodeIds,
                               Collection<Integer> threadIds) {
        super("Failed nodeIds: " + nodeIds + " threaIds: " + threadIds);
        this.nodeIds = nodeIds;
        this.threadIds = threadIds;
    }
    public NodeFailedException(Collection<Integer> nodeIds,
                               Collection<Integer> threadIds,
                               Exception cause) {
        super("Failed nodeIds: " + nodeIds + " threaIds: " + threadIds, cause);
        this.nodeIds = nodeIds;
        this.threadIds = threadIds;
    }
}
