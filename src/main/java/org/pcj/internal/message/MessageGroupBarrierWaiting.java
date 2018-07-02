/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal.message;

import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
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
final public class MessageGroupBarrierWaiting extends ReliableMessage {

    private int physicalId;
    private int groupId;
    private int barrierRound;

    public MessageGroupBarrierWaiting() {
        super(MessageType.GROUP_BARRIER_WAITING);
    }

    public MessageGroupBarrierWaiting(int groupId, int barrierRound, int physicalId) {
        this();

        this.groupId = groupId;
        this.barrierRound = barrierRound;
        this.physicalId = physicalId;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        writeFTData(out);
        out.writeInt(groupId);
        out.writeInt(barrierRound);
        out.writeInt(physicalId);

    }

    @Override
    protected void doExecute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        readFTData(in);
        groupId = in.readInt();
        barrierRound = in.readInt();
        physicalId = in.readInt();

        InternalCommonGroup group = InternalPCJ.getNodeData().getGroupById(groupId);

        GroupBarrierState barrierState = group.getBarrierState(barrierRound);
        barrierState.processPhysical(physicalId);
    }
}
