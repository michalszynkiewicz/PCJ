/*
 * Copyright (c) 2011-2017, PCJ Library
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package org.pcj.internal.message;

import org.pcj.PCJ;
import org.pcj.internal.ft.FailurePropagator;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 10/4/17
 */
public class MessageNodeFailureDetected extends Message {
    int failedNodePhysicalId;

    public MessageNodeFailureDetected() {
        super(MessageType.NODE_FAILURE_DETECTED);
    }

    public MessageNodeFailureDetected(int physicalId) {
        this();
        this.failedNodePhysicalId = physicalId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(failedNodePhysicalId);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        if (PCJ.getNodeId() != 0) {
            System.err.println("MessageNodeFailureDetected sent to a wrong node! " +
                    "Should be received by node 0 only");
        } else {
            failedNodePhysicalId = in.readInt();
            // TODO: wrong placement!
            // TODO: FailurePropagator should only be responsible for sending the info from nodes to master
            FailurePropagator.get().propagateError(failedNodePhysicalId);
        }
    }
}
