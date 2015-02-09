package org.pcj.internal.message;

import org.pcj.internal.network.MessageInputStream;
import org.pcj.internal.network.MessageOutputStream;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 08 Feb 2015
 */


public class MessageNodeRemoved extends Message {
    int failedNodePhysicalId;

    public MessageNodeRemoved() {
        super(MessageTypes.NODE_REMOVED);
    }

    public MessageNodeRemoved(int failedNodePhysicalId) {
        this();
        this.failedNodePhysicalId = failedNodePhysicalId;
    }

    @Override
    void writeObjects(MessageOutputStream bbos) {
        bbos.writeInt(failedNodePhysicalId);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        failedNodePhysicalId = bbis.readInt();
    }

    @Override
    public String paramsToString() {
        return null;
    }

    public int getFailedNodePhysicalId() {
        return failedNodePhysicalId;
    }
}

