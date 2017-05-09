package org.pcj.internal.message;

import org.pcj.PCJ;
import org.pcj.internal.faulttolerance.SetChild;
import org.pcj.internal.network.MessageInputStream;
import org.pcj.internal.network.MessageOutputStream;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 08 Feb 2015
 */


public class MessageNodeRemoved extends BroadcastedMessage {
    int failedNodePhysicalId;
    private List<SetChild> communicationUpdates = new ArrayList<>();

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

        bbos.writeInt(communicationUpdates.size());
        for (SetChild update : communicationUpdates) {
            update.writeObjects(bbos);
        }
    }

    @Override
    void readObjects(MessageInputStream bbis) {
        failedNodePhysicalId = bbis.readInt();

        int updatesSize = bbis.readInt();

        for (int i = 0; i < updatesSize; i++) {
            SetChild update = new SetChild();
            update.readObjects(bbis);
            communicationUpdates.add(update);
        }
    }

    public List<SetChild> getCommunicationUpdates() {
        return communicationUpdates;
    }

    @Override
    public String paramsToString() {
        return null;
    }

    public int getFailedNodePhysicalId() {
        return failedNodePhysicalId;
    }

    public void setCommunicationUpdates(List<SetChild> updates) {
        this.communicationUpdates = updates;
    }

    @Override
    public int getGroupId() {
        return 0; // this message is always broadcasted to all nodes
    }
}

