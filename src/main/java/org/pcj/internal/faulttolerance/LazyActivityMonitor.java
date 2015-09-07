package org.pcj.internal.faulttolerance;

import org.pcj.internal.message.MessagePing;
import org.pcj.internal.utils.Configuration;

import static org.pcj.internal.InternalPCJ.getNetworker;
import static org.pcj.internal.InternalPCJ.getWorkerData;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 9/7/15
 * Time: 4:25 PM
 */
public class LazyActivityMonitor implements Runnable {
    private final MessagePing messagePing = new MessagePing();

    @Override
    public void run() {
        while (sleep()) {
            pingParent();
            checkChildrenForFailure();
        }
    }

    private void pingParent() {
        getNetworker().sendToPhysicalNode(getWorkerData().getInternalGlobalGroup().getPhysicalParent(), messagePing);
        // mstodo handle parent pinging failure
    }

    private void checkChildrenForFailure() {
        // mstodo
    }

    private boolean sleep() {
        try {
            Thread.sleep(Configuration.NODE_PING_INTERVAL);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
