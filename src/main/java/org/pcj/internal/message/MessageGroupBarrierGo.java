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
import org.pcj.internal.ft.ReliableMessageCache;
import org.pcj.internal.futures.GroupBarrierState;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageGroupBarrierGo extends ReliableMessage {

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
    public void doWrite(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(barrierRound);
    }

    @Override
    protected void doExecute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        barrierRound = in.readInt();
        handle();
    }

    private void handle() {
        NodeData nodeData = InternalPCJ.getNodeData();

        InternalCommonGroup group = nodeData.getGroupById(groupId);

        List<Integer> childrenNodes = group.getChildrenNodes();
        System.out.println("[" + PCJ.getNodeId() + "] handling group go " + barrierRound + ", children: " + group.getChildrenNodes());
        ReliableMessageCache.get().add(this, this::handle);
        childrenNodes.stream()
                .peek(nodeId -> System.out.println("[" + PCJ.getNodeId() + "] resending to: " + nodeId))
                .map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> Emitter.get().send(socket, this));
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
