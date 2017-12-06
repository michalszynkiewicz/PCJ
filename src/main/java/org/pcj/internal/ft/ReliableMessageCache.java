/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.ft;

import org.pcj.internal.Configuration;
import org.pcj.internal.message.ReliableMessage;

import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Caches messages that should be resent in case of a node failure
 *
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 2/12/18
 */
public class ReliableMessageCache {
    public static final long timeToLive = Configuration.NODE_TIMEOUT * 10; //TODO: move to configuration?
    private List<ReliableMessageData> entries = new LinkedList<>();
    private BitSet processedMessages = new BitSet();

    // mstodo: thread-safety

    private static ReliableMessageCache instance = new ReliableMessageCache();

    public static ReliableMessageCache get() {
        return instance;
    }

    public void add(ReliableMessage message) {
        long now = getNow();
        removeOldOnes(now);
        entries.add(new ReliableMessageData(message, getNow()));
        processedMessages.set(message.getMessageId());
    }

    public void add(ReliableMessage message, Runnable failureHandler) {
        long now = getNow();
        removeOldOnes(now);
        entries.add(new ReliableMessageData(message, failureHandler, getNow()));
        processedMessages.set(message.getMessageId());
    }

    private long getNow() {
        return new Date().getTime();
    }

    public List<ReliableMessage> getList() {
        long now = getNow();
        removeOldOnes(now);

        return entries.stream().map(e -> e.message).collect(Collectors.toList());
    }

    public boolean isProcessed(ReliableMessage message) {
        return processedMessages.get(message.getMessageId());
    }

    private void removeOldOnes(long now) {
        long expiryDate = now - timeToLive;
        Iterator<ReliableMessageData> iterator = entries.iterator();
        while (iterator.hasNext()) {
            ReliableMessageData entry = iterator.next();
            if (entry.time < expiryDate) {
                iterator.remove();
            } else {
                break;
            }
        }

    }

    private static class ReliableMessageData {
        private static final Runnable DO_NOTHING = () -> {
        };

        private final ReliableMessage message;
        private final Runnable handler;
        private final long time;

        private ReliableMessageData(ReliableMessage message,
                                    Runnable handler,
                                    long time) {
            this.message = message;
            this.handler = handler;
            this.time = time;
        }

        private ReliableMessageData(ReliableMessage message, long time) {
            this(message, DO_NOTHING, time);
        }
    }
}
