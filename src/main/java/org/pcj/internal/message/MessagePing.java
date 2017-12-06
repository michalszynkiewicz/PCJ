package org.pcj.internal.message;

import org.pcj.PCJ;
import org.pcj.internal.ft.ActivityMonitor;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 3/8/18
 */
public class MessagePing extends Message {
    public MessagePing() {
        super(MessageType.PING);
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        int senderPhysicalId = PCJ.getNodeData().getPhysicalId(sender);
        ActivityMonitor.get().pingFromChild(senderPhysicalId);
    }
}
