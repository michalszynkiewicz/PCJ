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
import org.pcj.internal.ft.Emitter;
import org.pcj.internal.ft.NodeReconfigurator;
import org.pcj.internal.ft.ReliableMessageCache;
import org.pcj.internal.ft.SetChild;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import static org.pcj.internal.InternalPCJ.getNodeData;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 10/4/17
 */
public class MessageNodeRemoved extends ReliableMessage {
    private int failedNodePhysicalId;
    private List<SetChild> communicationUpdates = new ArrayList<>();


    public MessageNodeRemoved() {
        super(MessageType.NODE_REMOVED);
    }

    public List<SetChild> getSetChilds() {
        return communicationUpdates;
    }

    public void setSetChilds(List<SetChild> updates) {
        this.communicationUpdates = updates;
    }

    public int getFailedNodePhysicalId() {
        return failedNodePhysicalId;
    }

    public void setFailedNodePhysicalId(int failedNodePhysicalId) {
        this.failedNodePhysicalId = failedNodePhysicalId;
    }


    @Override
    public void doWrite(MessageDataOutputStream out) throws IOException {
        out.writeInt(failedNodePhysicalId);
        out.writeObject(communicationUpdates);
    }

    @Override
    protected void doExecute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        try {
            failedNodePhysicalId = in.readInt();
            communicationUpdates = (List<SetChild>) in.readObject();
            boolean shouldReplay =
                    NodeReconfigurator.get().handleNodeRemoved(failedNodePhysicalId, communicationUpdates);
            if (shouldReplay) {
                System.out.println("[" + PCJ.getNodeId() + "] will replay messages");
                ReliableMessageCache.get().replay();
            }
            sendOut();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to find class to read MessageNodeRemoved", e);
        }
    }

    private void sendOut() {
        getNodeData().getGlobalGroup().getChildrenNodes()
                .forEach(nodeId -> {
                    System.out.println("[" + PCJ.getNodeId() + "] (re?)emitting node removed to " + nodeId); // mstodo remove
                    Emitter.get().sendAndPerformOnFailure(nodeId, this, this::sendOut);
                });
    }

    @Override
    public String toString() {
        return "MessageNodeRemoved{" +
                "failedNodePhysicalId=" + failedNodePhysicalId +
                ", communicationUpdates=" + communicationUpdates +
                '}';
    }
}
