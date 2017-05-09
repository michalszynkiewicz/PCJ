package org.pcj.internal.faulttolerance;

import org.pcj.internal.message.BroadcastedMessage;

import java.util.*;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/20/15
 * Time: 2:08 PM
 */
public class BroadcastCache {

    public static final long timeToLive = 5000; //[ms] todo: move to configuration
    private List<Entry> entries = new LinkedList<>();
    private Set<Integer> processedMessages = new HashSet<>();

    public void add(BroadcastedMessage message) {
        long now = getNow();
        removeOldOnes(now);

        Entry e = new Entry();
        e.message = message;
        e.time = now;
        entries.add(e);
        processedMessages.add(message.getMessageId());
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
        return processedMessages.contains(message.getMessageId());
    }

    private void removeOldOnes(long now) {
        long expiryDate = now - timeToLive;
        Iterator<Entry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
//            mstodo a candidate for clean up later on:
//            processedMessages.remove(entry.message.getMessageId());
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
