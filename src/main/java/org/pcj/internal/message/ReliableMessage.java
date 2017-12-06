/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message;

import org.pcj.PCJ;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/23/17
 */
public abstract class ReliableMessage extends Message {
    // mstodo cache'em and be sure not to process multiple times
    private static final AtomicInteger idGenerator = new AtomicInteger();

    private AtomicInteger messageId = new AtomicInteger(-1);
    private Integer originator = PCJ.getNodeId();

    ReliableMessage(MessageType type) {
        super(type);
    }

    // TODO: messageId generation is a bit overcomplicated, consider simplifying
    public Integer getMessageId() {
        messageId.compareAndSet(-1, idGenerator.getAndIncrement());
        return messageId.get();
    }

    public Integer getOriginator() {
        return originator;
    }


    protected void writeFTData(MessageDataOutputStream out) throws IOException {
        out.writeInt(getMessageId());
        out.writeInt(originator);
    }

    protected void readFTData(MessageDataInputStream in) throws IOException {
        messageId.set(in.readInt());
        originator = in.readInt();
    }
}
