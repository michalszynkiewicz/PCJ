package org.pcj.internal.message;

import org.pcj.internal.network.MessageInputStream;
import org.pcj.internal.network.MessageOutputStream;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 11/19/14
 * Time: 10:46 PM
 */
final public class MessagePing extends Message {
    private int sourceNode = -1;

    public MessagePing() {
        super(MessageTypes.PING);
    }
    public MessagePing(int sourceNode) {
        super(MessageTypes.PING);
        this.sourceNode = sourceNode;
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(sourceNode);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        this.sourceNode = bbis.readInt();
    }

    @Override
    public String paramsToString() {
        return null;
    }
}
