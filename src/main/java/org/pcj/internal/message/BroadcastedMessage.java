package org.pcj.internal.message;

/**
 * mstodo: is this one necessary?
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 1/21/18
 */
public abstract class BroadcastedMessage extends ReliableMessage {
    BroadcastedMessage(MessageType type) {
        super(type);
    }
}
