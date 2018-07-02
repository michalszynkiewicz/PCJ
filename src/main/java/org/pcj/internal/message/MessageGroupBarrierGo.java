/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message;

import org.pcj.PCJ;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.ft.Emitter;
import org.pcj.internal.futures.GroupBarrierState;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageGroupBarrierGo extends BroadcastedMessage {

    private int groupId;
    private int barrierRound;

    public MessageGroupBarrierGo() {
        super(MessageType.GROUP_BARRIER_GO);
    }

    public MessageGroupBarrierGo(int groupId, int barrierRound) {
        this();

        this.groupId = groupId;
        this.barrierRound = barrierRound;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        writeFTData(out);
        out.writeInt(groupId);
        out.writeInt(barrierRound);
    }

    @Override
    protected void doExecute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        readFTData(in);
        groupId = in.readInt();
        barrierRound = in.readInt();
        handle();
    }

    private void handle() {
        NodeData nodeData = InternalPCJ.getNodeData();

        InternalCommonGroup group = nodeData.getGroupById(groupId);

        group.getChildrenNodes().stream()
                .peek(nodeId -> System.out.println("[" + PCJ.getNodeId() + "] resending to: " + nodeId))
                .map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> Emitter.get().sendAndPerformOnFailure(socket, this, this::handle));
        // mstodo check if the group children are set properly here

        GroupBarrierState barrier = group.removeBarrierState(barrierRound);
        if (barrier == null) {
            System.out.println("attempt to signal done a non-existing barrier, probably due to message replay");
        } else {
            System.out.println("[" + PCJ.getNodeId() + "] signalling " + barrierRound);
            barrier.signalDone();
        }
    }
}
