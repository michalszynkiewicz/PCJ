package org.pcj.internal.message;

import org.pcj.internal.network.MessageInputStream;
import org.pcj.internal.network.MessageOutputStream;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 11/19/14
 * Time: 10:46 PM
 */
final public class MessagePing extends Message {
    private int groupId;
    public MessagePing() {
        super(MessageTypes.PING);
    }
    public MessagePing(int groupId) {
        this();
        this.groupId = groupId;
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(groupId);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        groupId = bbis.readInt();
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    @Override
    public String paramsToString() {
        return null;
    }
}
