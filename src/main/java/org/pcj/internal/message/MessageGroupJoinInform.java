/* 
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.pcj.internal.InternalCommonGroup;
import org.pcj.internal.InternalPCJ;
import org.pcj.internal.NodeData;
import org.pcj.internal.futures.GroupJoinState;
import static org.pcj.internal.message.Message.LOGGER;
import org.pcj.internal.network.MessageDataInputStream;
import org.pcj.internal.network.MessageDataOutputStream;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class MessageGroupJoinInform extends Message {

    private int requestNum;
    private int groupId;
    private int globalThreadId;
    private Map<Integer, Integer> threadsMapping;

    public MessageGroupJoinInform() {
        super(MessageType.GROUP_JOIN_INFORM);
    }

    public MessageGroupJoinInform(int requestNum, int groupId, int globalThreadId,
            Map<Integer, Integer> threadsMapping) {
        this();

        this.requestNum = requestNum;
        this.groupId = groupId;
        this.globalThreadId = globalThreadId;
        this.threadsMapping = new HashMap<>(threadsMapping);
    }

    @Override
    public void write(MessageDataOutputStream out) throws IOException {
        out.writeInt(requestNum);
        out.writeInt(groupId);
        out.writeInt(globalThreadId);
        out.writeObject(threadsMapping);
    }

    @Override
    public void execute(SocketChannel sender, MessageDataInputStream in) throws IOException {
        requestNum = in.readInt();
        groupId = in.readInt();
        globalThreadId = in.readInt();
//        System.out.println(groupId + ": " + InternalPCJ.getNodeData().getPhysicalId() + " received inform num:" + requestNum + ", glId:" + globalThreadId);

        try {
            Object obj = in.readObject();
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Integer, Integer> map = (Map<Integer, Integer>) obj;
                threadsMapping = map;
            }
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Unable to read threadsMapping", ex);
            throw new RuntimeException(ex);
        }

        NodeData nodeData = InternalPCJ.getNodeData();
        InternalCommonGroup commonGroup = nodeData.getGroupById(groupId);

        List<Integer> keys = new ArrayList<>(threadsMapping.keySet());
        keys.sort(Integer::compare);
        for (int groupThreadId : keys) {
            int globalId = threadsMapping.get(groupThreadId);

            commonGroup.addThread(globalId, groupThreadId);
        }

        CopyOnWriteArrayList<Integer> physicalIds = new CopyOnWriteArrayList<>();
        List<Integer> childrenNodes = threadsMapping.keySet().stream()
                .sorted()
                .mapToInt(threadsMapping::get)
                .map(nodeData::getPhysicalId)
                .filter(physicalIds::addIfAbsent)
                .map(physicalIds::lastIndexOf)
                .filter(index -> index > 0)
                .filter(index -> physicalIds.get((index - 1) / 2) == nodeData.getPhysicalId())
                .map(physicalIds::get)
                .boxed().collect(Collectors.toList());
//        System.out.println("itm:"+threadsMapping+" cg:"+commonGroup.getChildrenNodes()+ " cn:"+childrenNodes);
        GroupJoinState groupJoinState = commonGroup.getGroupJoinState(requestNum, globalThreadId, childrenNodes);

        childrenNodes.stream()
                //                .peek(el -> System.out.println(groupId + ": " + InternalPCJ.getNodeData().getPhysicalId() + " broadcasting this inform to " + el + " num:" + requestNum + ", glId:" + globalThreadId + " tm:" + threadsMapping))
                .map(nodeData.getSocketChannelByPhysicalId()::get)
                .forEach(socket -> InternalPCJ.getNetworker().send(socket, this));

        if (groupJoinState.processPhysical(nodeData.getPhysicalId())) {
            int requesterPhysicalId = nodeData.getPhysicalId(globalThreadId);
            if (requesterPhysicalId != nodeData.getPhysicalId()) {
                commonGroup.removeGroupJoinState(requestNum, globalThreadId);
            }
        }
    }
}
