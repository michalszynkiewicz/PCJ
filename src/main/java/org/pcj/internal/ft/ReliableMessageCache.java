/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.ft;

import org.pcj.PCJ;
import org.pcj.internal.Configuration;
import org.pcj.internal.message.ReliableMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private Set<MessageId> messageIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // mstodo: thread-safety

    private static ReliableMessageCache instance = new ReliableMessageCache();

    public static ReliableMessageCache get() {
        return instance;
    }

    public void add(ReliableMessage message) {
        long now = getNow();
        removeOldOnes(now);
        entries.add(new ReliableMessageData(message, getNow()));
    }

    public void add(ReliableMessage message, Runnable failureHandler) {
        long now = getNow();
        removeOldOnes(now);
        entries.add(new ReliableMessageData(message, failureHandler, getNow()));
    }

    public void markProcessed(ReliableMessage message) {
        messageIds.add(new MessageId(message));
    }

    private long getNow() {
        return new Date().getTime();
    }

    private List<ReliableMessageData> getList() {
        long now = getNow();
        removeOldOnes(now);

        return new ArrayList<>(entries); // mstodo might be unnecessary
    }

    public boolean isProcessed(ReliableMessage message) {
        return messageIds.contains(new MessageId(message));
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

    public void replay() {
         getList().forEach(ReliableMessageData::handle);
    }

    private static class ReliableMessageData {
        // mstodo do nothing makes no sense, a reasonable default should probably be "send to children"
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

        private void handle() {
            System.out.println("[" + PCJ.getNodeId() +"] resending message " + message);
            handler.run();
        }
    }

    private static class MessageId {
        private final Integer messageId;
        private final Integer originator;
        private MessageId(ReliableMessage message) {
            messageId = message.getMessageId();
            originator = message.getOriginator();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessageId messageId1 = (MessageId) o;
            return Objects.equals(messageId, messageId1.messageId) &&
                    Objects.equals(originator, messageId1.originator);
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageId, originator);
        }

        @Override
        public String toString() {
            return "MessageId{" +
                    "messageId=" + messageId +
                    ", originator=" + originator +
                    '}';
        }
    }
}
