package org.pcj.internal.message;

import org.pcj.PCJ;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 7/8/18
 */
public class MessageBarrierReached extends Message {

    private int round;
    private boolean finished;
    private String groupName;

    public MessageBarrierReached() {
        super(MessageType.BARRIER_REACHED);
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(round);
        out.writeBoolean(finished);
        out.writeString(groupName);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        round = in.readInt();
        finished = in.readBoolean();
        groupName = in.readString();

        int senderId = PCJ.getNodeData().getPhysicalId(sender);

        InternalPCJ.getNodeData().getGroups()
                .stream()
                .filter(g -> g.getGroupName().equals(groupName))
                .findAny()
                .ifPresent(
                        group ->
                                group.markBarrierReached(round, finished, senderId)
                );
    }

    public void setRound(int latestReachedBarrier) {
        this.round = latestReachedBarrier;
    }

    public void setFinished(boolean barrierFinished) {
        this.finished = barrierFinished;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
