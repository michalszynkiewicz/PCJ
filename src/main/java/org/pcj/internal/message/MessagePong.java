package org.pcj.internal.message;

import org.pcj.internal.network.MessageInputStream;
import org.pcj.internal.network.MessageOutputStream;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 12/2/14
 * Time: 11:34 AM
 */
final public class MessagePong extends Message {

    private int groupId;

    public MessagePong() {
        super(MessageTypes.PONG);
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
