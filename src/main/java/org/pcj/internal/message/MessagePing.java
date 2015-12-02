package org.pcj.internal.message;

import org.pcj.internal.network.MessageInputStream;
import org.pcj.internal.network.MessageOutputStream;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 11/19/14
 * Time: 10:46 PM
 */
final public class MessagePing extends Message {
    public MessagePing() {
        super(MessageTypes.PING);
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
    }

    @Override
    void readObjects(MessageInputStream bbis) {
    }

    @Override
    public String paramsToString() {
        return null;
    }
}
