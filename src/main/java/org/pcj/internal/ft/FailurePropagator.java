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
import org.pcj.internal.NodeData;
import org.pcj.internal.message.Message;
import org.pcj.internal.message.MessageBye;
import org.pcj.internal.message.MessageNodeFailureDetected;
import org.pcj.internal.message.MessageNodeRemoved;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Set;

import static org.pcj.internal.ConfigurationLock.runUnderReadLock;
import static org.pcj.internal.InternalPCJ.getNetworker;
import static org.pcj.internal.InternalPCJ.getNodeData;

/**
 * {@code NodeFailurePropagator} is responsible for reconfiguring
 * the internal structures after a  node failure
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 10/4/17
 */
public class FailurePropagator {
    private CommunicationNode tree;

    private FailurePropagator() {
    }

    public void init(Set<Integer> physicalIds) {
        if (PCJ.getNodeId() == 0) {
            tree = CommunicationNode.build(0, physicalIds.size(), null);
        }
    }

    public void notifyAboutFailure(Integer failedPhysicalId, IOException e) {
        if (e != null) { // mstodo prettify this
            e.printStackTrace();
        }

        runUnderReadLock(
                () -> sendToRootNode(new MessageNodeFailureDetected(failedPhysicalId))
        );
    }

    public void notifyAboutFailure(SocketChannel socket, IOException e) {
        Integer physicalId = getNodeData().getPhysicalId(socket);
        notifyAboutFailure(physicalId, e);
    }

    public void propagateError(Integer failedPhysicalId) {
        runUnderReadLock(() -> {
                    if (tree.contains(failedPhysicalId)) {
                        sendToRootNode(prepareReconfigurationMessage(failedPhysicalId));
                        sendNodeFinish(failedPhysicalId);
                    } else {
                        System.out.println("Ignoring duplicate information about node failure " +
                                "for node " + failedPhysicalId);
                    }
                }
        );
    }

    private void sendNodeFinish(Integer failedPhysicalId) {
        MessageBye bye = new MessageBye(failedPhysicalId);
        Emitter.get().send(0, bye);
    }

    private MessageNodeRemoved prepareReconfigurationMessage(Integer failedPhysicalId) {
        List<SetChild> updates = CommunicationTreeFixer.remove(tree, failedPhysicalId);

        MessageNodeRemoved message = new MessageNodeRemoved();
        message.setSetChilds(updates);
        message.setFailedNodePhysicalId(failedPhysicalId);
        System.out.println("will send a node remove message: " + message);
        return message;
    }

    private void sendToRootNode(Message message) {
        NodeData nodeData = getNodeData();
        int parentNode = nodeData.getGlobalGroup().getGroupMasterNode();
        SocketChannel socket = nodeData.getSocketChannelByPhysicalId().get(parentNode);
        try {
            getNetworker().send(socket, message);
        } catch (IOException e) {
            System.out.println("Node 0 failed, there is no hope, exiting");
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static FailurePropagator instance;

    static {
        instance = new FailurePropagator();
    }

    public static FailurePropagator get() {
        return instance;
    }
}
