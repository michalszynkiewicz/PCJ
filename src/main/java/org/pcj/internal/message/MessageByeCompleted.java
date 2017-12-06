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
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.ft.Emitter;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Message sent by node0 to all nodes about completion of execution.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageByeCompleted extends BroadcastedMessage {

    public MessageByeCompleted() {
        super(MessageType.BYE_COMPLETED);
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        writeFTData(out);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        readFTData(in);

        NodeData nodeData = InternalPCJ.getNodeData();
        PCJ.getNodeData()
                .getGlobalGroup()
                .getChildrenNodes().forEach(
                childId ->
                        Emitter.get().send(nodeData.getSocketChannelByPhysicalId().get(childId), this)
        );


        nodeData.getGlobalWaitObject().signal();
    }
}
