package org.pcj.internal.message;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/20/15
 * Time: 2:43 PM
 */
public abstract class BroadcastedMessage extends Message {
    BroadcastedMessage(MessageTypes type) {
        super(type);
    }

    public abstract int getGroupId();
}
