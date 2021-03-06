package org.pcj.internal.message;

import org.pcj.internal.network.MessageInputStream;
import org.pcj.internal.network.MessageOutputStream;

/**
 * Message sent to node0 when a node failure was discovered
 *
 * (used to propagate failure)
 *
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 1/26/15
 * Time: 10:28 PM
 */
public class MessageNodeFailed extends Message {
    int failedNodePhysicalId;
    public MessageNodeFailed() {
        super(MessageTypes.NODE_FAILED);
    }

    public MessageNodeFailed(int failedNodePhysicalId) {
        this();
        this.failedNodePhysicalId = failedNodePhysicalId;
    }

    public int getFailedNodePhysicalId() {
        return failedNodePhysicalId;
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
}
