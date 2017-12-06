/*
 * Copyright (c) 2011-2017, PCJ Library
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package org.pcj.internal.message;

import org.pcj.internal.ft.NodeReconfigurator;
import org.pcj.internal.ft.SetChild;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

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
    public void write(MessageDataOutputStream out) throws IOException {
        writeFTData(out);
        out.writeInt(failedNodePhysicalId);
        out.writeObject(communicationUpdates);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        readFTData(in);
        try {
            failedNodePhysicalId = in.readInt();
            communicationUpdates = (List<SetChild>) in.readObject();
            NodeReconfigurator.get().handleNodeRemoved(failedNodePhysicalId, communicationUpdates);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to find class to read MessageNodeRemoved", e);
        }
    }

    @Override
    public String toString() {
        return "MessageNodeRemoved{" +
                "failedNodePhysicalId=" + failedNodePhysicalId +
                ", communicationUpdates=" + communicationUpdates +
                '}';
    }
}
