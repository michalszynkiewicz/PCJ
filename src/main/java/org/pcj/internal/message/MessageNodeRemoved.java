package org.pcj.internal.message;

import org.pcj.internal.network.MessageInputStream;
import org.pcj.internal.network.MessageOutputStream;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 08 Feb 2015
 */


public class MessageNodeRemoved extends Message {
    int failedNodePhysicalId;
    private int newCommunicationNode;
    private Integer newCommunicationLeft;
    private Integer newCommunicationRight;

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

        bbos.writeInt(newCommunicationNode);
        bbos.writeInt(newCommunicationLeft == null ? -1 : newCommunicationLeft);
        bbos.writeInt(newCommunicationRight == null ? -1 : newCommunicationRight);
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        failedNodePhysicalId = bbis.readInt();

        newCommunicationNode = bbis.readInt();
        newCommunicationLeft = bbis.readInt();
        newCommunicationRight = bbis.readInt();

        if (newCommunicationLeft == -1) {
            newCommunicationLeft = null;
        }
        if (newCommunicationRight == -1) {
            newCommunicationRight = null;
        }
    }

    @Override
    public String paramsToString() {
        return null;
    }

    public int getFailedNodePhysicalId() {
        return failedNodePhysicalId;
    }


    public void setNewCommunicationNode(int newCommunicationNode) {
        this.newCommunicationNode = newCommunicationNode;
    }

    public int getNewCommunicationNode() {
        return newCommunicationNode;
    }

    public void setNewCommunicationLeft(Integer newCommunicationLeft) {
        this.newCommunicationLeft = newCommunicationLeft;
    }

    public void setNewCommunicationRight(Integer newCommunicationRight) {
        this.newCommunicationRight = newCommunicationRight;
    }

    public Integer getNewCommunicationLeft() {
        return newCommunicationLeft;
    }

    public Integer getNewCommunicationRight() {
        return newCommunicationRight;
    }
}

