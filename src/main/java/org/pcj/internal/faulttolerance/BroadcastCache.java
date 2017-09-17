package org.pcj.internal.faulttolerance;

import org.pcj.internal.message.BroadcastedMessage;
import org.pcj.internal.message.MessageSyncGo;
import org.pcj.internal.utils.Configuration;

import java.util.*;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/20/15
 * Time: 2:08 PM
 */
public class BroadcastCache {

    public static final long timeToLive = Configuration.NODE_TIMEOUT * 10; //[ms] todo: move to configuration
    private List<Entry> entries = new LinkedList<>();
    private BitSet processedMessages = new BitSet();

    public void add(BroadcastedMessage message) {
        long now = getNow();
        removeOldOnes(now);

        Entry e = new Entry();
        e.message = message;
        e.time = now;
        entries.add(e);
        processedMessages.set(message.getMessageId());
    }

    public List<BroadcastedMessage> getList() {
        long now = getNow();
        removeOldOnes(now);

        List<BroadcastedMessage> resultList = new ArrayList<>();
        for (Entry entry : entries) {
            resultList.add(entry.message);
        }

        return resultList;
    }

    public boolean isProcessed(BroadcastedMessage message) {
        return processedMessages.get(message.getMessageId());
    }

    private void removeOldOnes(long now) {
        long expiryDate = now - timeToLive;
        Iterator<Entry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.time < expiryDate) {
                iterator.remove();
            } else {
                break;
            }
        }

    }

    private long getNow() {
        return new Date().getTime();
    }

    private static class Entry {
        BroadcastedMessage message;
        long time;
    }
}
