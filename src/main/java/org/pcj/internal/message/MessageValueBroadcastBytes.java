/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message;

import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStorages;
import org.pcj.internal.NodeData;
import org.pcj.internal.PcjThread;
import org.pcj.internal.ft.Emitter;
import org.pcj.internal.network.CloneInputStream;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * ....
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
final public class MessageValueBroadcastBytes extends BroadcastedMessage {

    private int requestNum;
    private int groupId;
    private int requesterThreadId;
    private String sharedEnumClassName;
    private String name;
    private int[] indices;
    private CloneInputStream clonedData;

    public MessageValueBroadcastBytes() {
        super(MessageType.VALUE_BROADCAST_BYTES);
    }

    public MessageValueBroadcastBytes(int groupId, int requestNum, int requesterThreadId, String storageName, String name, int[] indices, CloneInputStream clonedData) {
        this();

        this.groupId = groupId;
        this.requestNum = requestNum;
        this.requesterThreadId = requesterThreadId;
        this.sharedEnumClassName = storageName;
        this.name = name;
        this.indices = indices;

        this.clonedData = clonedData;
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(groupId);
        out.writeInt(requestNum);
        out.writeInt(requesterThreadId);
        out.writeString(sharedEnumClassName);
        out.writeString(name);
        writeFTData(out);
        out.writeIntArray(indices);

        clonedData.writeInto(out);
    }

    @Override
    protected void doExecute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        read(in);

        handle();
        return;
    }

    private void handle() {
        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup group = nodeData.getGroupById(groupId);

        List<Integer> children = group.getChildrenNodes();

        MessageValueBroadcastBytes message
                = new MessageValueBroadcastBytes(groupId, requestNum, requesterThreadId,
                        sharedEnumClassName, name, indices, clonedData);

        children.stream().map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> Emitter.get().send(socket, message));

        Queue<Exception> exceptionsQueue = new LinkedList<>();
        int[] threadsId = group.getLocalThreadsId();
        for (int threadId : threadsId) {
            try {
                int globalThreadId = group.getGlobalThreadId(threadId);
                PcjThread pcjThread = nodeData.getPcjThread(globalThreadId);
                InternalStorages storage = (InternalStorages) pcjThread.getThreadData().getStorages();

                clonedData.reset();
                Object newValue = new ObjectInputStream(clonedData).readObject();

                storage.put(newValue, sharedEnumClassName, name, indices);
            } catch (Exception ex) {
                exceptionsQueue.add(ex);
            }
        }

        int globalThreadId = group.getGlobalThreadId(requesterThreadId);
        int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
        SocketChannel socket = InternalPCJ.getNodeData().getSocketChannelByPhysicalId().get(requesterPhysicalId);

        MessageValueBroadcastInform messageInform = new MessageValueBroadcastInform(groupId, requestNum, requesterThreadId,
                nodeData.getPhysicalId(), exceptionsQueue);
        Emitter.get().send(socket, messageInform);
    }

    private void read(MessageDataInputStream in) throws IOException {
        groupId = in.readInt();
        requestNum = in.readInt();
        requesterThreadId = in.readInt();

        sharedEnumClassName = in.readString();
        name = in.readString();
        readFTData(in);
        indices = in.readIntArray();

        clonedData = CloneInputStream.readFrom(in);
    }

    public int getGroupId() {
        return groupId;
    }
}
