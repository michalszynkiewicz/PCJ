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
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.message.BroadcastedMessage;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.ReliableMessage;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Sends messages.
 *
 * Should be used instead of Networker because it caches the messages that should be sent reliably.
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 1/24/18
 */
public class Emitter {

    /**
     * Sends a message to a node with given physicalId.
     * If sending fails, the method succeeds, but the failure is propagated.
     * @param physicalNodeId target node
     * @param message message to send
     */
    public void send(Integer physicalNodeId, Message message) {
        SocketChannel socket = PCJ.getNodeData().getSocketChannelByPhysicalId().get(physicalNodeId);
        if (socket == null) {
            FaultToleranceHandler.get().fail(physicalNodeId);
        }
        send(socket, message);
    }

    /**
     * Sends a message to the socket.
     * If sending fails, the method succeeds, but the failure is propagated.
     * @param socket target socket
     * @param message message to send
     */
    public void send(SocketChannel socket, Message message) {
        if (message instanceof ReliableMessage) {
            ReliableMessageCache.get().add((BroadcastedMessage) message);
        }
        doSend(socket, message);
    }

    /**
     * Sends a message to the socket and registers a callback to be called when some node fails.
     * If sending fails, the method succeeds, but the failure is propagated.
     *
     * @param socket target socket
     * @param message message to send
     * @param failureHandler callback to execute when the message should be resend
     */
    public void sendAndPerformOnFailure(SocketChannel socket, Message message, Runnable failureHandler) {
        if (message instanceof ReliableMessage) {
            ReliableMessageCache.get().add((ReliableMessage) message, failureHandler);
        }
        doSend(socket, message);
    }

    private void doSend(SocketChannel socket, Message message) {
        try {
            InternalPCJ.getNetworker().send(socket, message);
        } catch (IOException e) {
            throw new RuntimeException("Error preparing message to send " + message, e);
        }
    }

    private static final Emitter instance = new Emitter();

    public static Emitter get() {
        return instance;
    }

}
