/*
 * This file is the internal part of the PCJ Library
 */
package org.pcj.internal;

import org.pcj.internal.faulttolerance.NodeFailedException;
import org.pcj.internal.message.BroadcastedMessage;
import org.pcj.internal.message.Message;
import org.pcj.internal.network.LoopbackSocketChannel;
import org.pcj.internal.network.MessageOutputStream;
import org.pcj.internal.network.SelectorProc;
import org.pcj.internal.utils.Configuration;
import org.pcj.internal.utils.Utilities;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.pcj.internal.InternalPCJ.getWorkerData;

/**
 * This is intermediate file (between classes that want to send data (eg.
 * {@link org.pcj.internal.Worker}) and
 * {@link org.pcj.internal.network.SelectorProc} classes) for sending data
 * across network. It is used for binding address, connecting to hosts and
 * sending data.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class Networker {        // mstodo: access rights!

    private final SelectorProc selectorProc;
    private final Thread selectorProcThread;
    private final Thread workerThread;
    private final WorkerData workerData;
    private final Worker worker;
    private final Deque<SelectableChannel> serverSockets;
    private boolean shuttingDown = false;

    Networker(Worker worker) throws IOException {
        this.worker = worker;
        this.workerData = worker.getData();
        this.selectorProc = new SelectorProc(worker);

        this.selectorProcThread = new Thread(selectorProc, "SelectorProc");
        this.selectorProcThread.setDaemon(true);

        this.workerThread = new Thread(worker, "Worker");
        this.workerThread.setDaemon(true);

        this.serverSockets = new ConcurrentLinkedDeque<>();
    }

    SocketChannel connectTo(InetAddress hostAddress, int port, int retry, int delay) throws IOException {

        for (int attempt = 0; attempt <= retry; ++attempt) {
            try {
                SocketChannel newNodeChannel = connectTo(hostAddress, port);
                waitForConnection(newNodeChannel);
                return newNodeChannel;
            } catch (IOException ex) {
                Utilities.sleep(delay * 1000);
                if (attempt + 1 >= retry) {
                    throw new RuntimeException("Connecting to " + hostAddress.getHostAddress() + ":" + port + " failed!", ex);
                }
            }
        }
        return null;
    }

    private SocketChannel connectTo(InetAddress hostAddress, int port) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);

//        socketChannel.connect(new InetSocketAddress(hostAddress, port));
        if (socketChannel.connect(new InetSocketAddress(hostAddress, port))) {
            selectorProc.connected(socketChannel);

            selectorProc.register(socketChannel, SelectionKey.OP_READ);
            
            synchronized (socketChannel) {
                socketChannel.notifyAll();
            }
        } else {
            selectorProc.register(socketChannel, SelectionKey.OP_CONNECT);
        }

        return socketChannel;
    }

    ServerSocketChannel bind(InetAddress hostAddress, int port) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverSocketChannel.configureBlocking(false);

        InetSocketAddress isa;
        if (hostAddress == null) {
            isa = new InetSocketAddress(port);
        } else {
            isa = new InetSocketAddress(hostAddress, port);
        }

        serverSocketChannel.socket().bind(isa, 4096);

        selectorProc.register(serverSocketChannel, SelectionKey.OP_ACCEPT);

        serverSockets.add(serverSocketChannel);

        return serverSocketChannel;
    }

    private void waitForConnection(SocketChannel socket) throws IOException {
        synchronized (socket) {
            if (selectorProc.isConnected(socket)) {
                return;
            }
            try {
                socket.wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
            if (selectorProc.isConnected(socket) == false) {
                throw new IOException("Unable to connect!");
            }
        }
    }

    private ByteBuffer prepareByteBuffer(final Message msg) {
        MessageOutputStream bbos = new MessageOutputStream();
        bbos.writeInt(0x00000000);

        msg.writeToOutputStream(bbos);

        ByteBuffer buf = bbos.getByteBuffer();

        int length = buf.limit() - 4; // (Integer.SIZE / Byte.SIZE)
        buf.putInt(length);
        buf.rewind();

        return buf;
    }

    public void broadcast(BroadcastedMessage message) {
        InternalGroup group = getWorkerData().internalGroupsById.get(message.getGroupId());
        try {
            broadcast(group, message);
        } catch (NodeFailedException nfe) {
            nfe.printStackTrace();
            // do nothing, the message will be replayed when the node failure is handled
        }
    }

    protected void broadcast(InternalGroup group, Message message) {
//        System.err.println("broadcast:" + message + " to " + group.getGroupName());
        Integer leftChildrenIndex = group.getPhysicalLeft();
        Integer rightChildrenIndex = group.getPhysicalRight();

        SocketChannel left = null;
        if (leftChildrenIndex != null) {
            left = getWorkerData().physicalNodes.get(leftChildrenIndex);
        }

        SocketChannel right = null;
        if (rightChildrenIndex != null) {
            right = getWorkerData().physicalNodes.get(rightChildrenIndex);
        }
        try {
            broadcast(left, right, message);    // mstodo verify!
        } catch (NodeFailedException nfe) {
            nfe.printStackTrace();         // mstodo add some info about continuing
            // do nothing, the message will be replayed when the node failure is handled
        }
    }

    public void broadcast(SocketChannel left, SocketChannel right, Message message) {
        if ((Configuration.DEBUG & 2) == 2) {
           if ((Configuration.DEBUG & 4) == 4) {
              System.err.println("" + worker.getData().physicalId + " broadcast: " + message + " to " + left + " and " + right);
           } else {
              System.err.println("" + worker.getData().physicalId + " broadcast: " + message.getType() + " to " + left + " and " + right);
           }
        }
       IOException exception = null;
       ByteBuffer mbuf = null;

//            if (message instanceof BroadcastedMessage) {
//                 LogUtils.log(InternalPCJ.getWorkerData().physicalId,
//                        "adding message to broadcast cache [" + message.getType() + "]: " + message.getMessageId());
//                workerData.broadcastCache.add((BroadcastedMessage) message);
//            }

       try {
          if (left != null) {
             if (left instanceof LoopbackSocketChannel) {
                worker.enqueueMessage(left, message);
             } else {
                if (mbuf == null) {
                   mbuf = prepareByteBuffer(message);
                }
                selectorProc.send(left, mbuf.duplicate());
             }
          }
       } catch (IOException e) {
          exception = e;
       }
       try {
          if (right != null && right != left) {
             if (right instanceof LoopbackSocketChannel) {
                worker.enqueueMessage(right, message);
             } else {
                if (mbuf == null) {
                   mbuf = prepareByteBuffer(message);
                }
                selectorProc.send(right, mbuf.duplicate());
             }
          }
//            for (SocketChannel child : left) {
//                if (child != null) {
//                    selectorProc.send(child, mbuf.duplicate());
//                }
//            }
       } catch (IOException ex) {
          exception = ex;
       }
       if (exception != null) {
          System.err.println("error broadcasting message: " + message);  // mstodo remove/replace with better exception
          throw new NodeFailedException(exception);
       }
    }

    void send(SocketChannel socket, Message message) throws IOException {
        if ((Configuration.DEBUG & 2) == 2) {
            if ((Configuration.DEBUG & 4) == 4) {
                System.err.println("" + worker.getData().physicalId + " send: " + message + " to " + socket.getRemoteAddress());
            } else {
                System.err.println("" + worker.getData().physicalId + " send: " + message.getType() + " to " + socket.getRemoteAddress());
            }
        }
        if (socket instanceof LoopbackSocketChannel) {
            worker.enqueueMessage(socket, message);
        } else {
            ByteBuffer mbuf = prepareByteBuffer(message);

            selectorProc.send(socket, mbuf);
        }
    }

    <X> X sendWait(SocketChannel socket, Message msg) throws IOException {
        int messageId = msg.getMessageId();

        InternalResponseAttachment wr = new InternalResponseAttachment();
        workerData.attachmentMap.put(messageId, wr);

        synchronized (wr) {
            send(socket, msg);

            return wr.waitForResponse();
        }
    }

    void startup() {
        selectorProcThread.start();
        workerThread.start();
    }

    void shutdown() {
        if (shuttingDown == true) {
            return;
        }
        shuttingDown = true;

        try {
            selectorProc.close();
            while (serverSockets.isEmpty() == false) {
                serverSockets.poll().close();
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        System.out.println("SHUTTING DOWN"); //mstodo remove
        selectorProcThread.interrupt();
        workerThread.interrupt();

//        System.exit(0);
    }

    public void send(InternalGroup group, Message msg) throws IOException {
//        Integer physicalRoot = group.getPhysicalMaster();
//
//        SocketChannel socket = null;
//        if (physicalRoot != null) {
//            socket = workerData.physicalNodes.getFutureObject(physicalRoot);
//        }
        SocketChannel socket = workerData.physicalNodes.get(group.getPhysicalMaster());
        send(socket, msg);
    }

    void send(int nodeId, Message msg) throws IOException {
        Integer physicalNodeId = workerData.virtualNodes.get(nodeId);
        if (physicalNodeId == null) {
            throw new NodeFailedException();
        }
        sendToPhysicalNode(physicalNodeId, msg);
    }

    public void sendToPhysicalNode(int physicalNodeId, Message message) throws IOException {
        SocketChannel socket = workerData.physicalNodes.get(physicalNodeId);
        if (socket == null) {
            throw new NodeFailedException(physicalNodeId);
        }
        send(socket, message);
    }

    <X> X sendWait(int nodeId, Message msg) throws IOException {
        SocketChannel socket = workerData.physicalNodes.get(workerData.virtualNodes.get(nodeId));
        return sendWait(socket, msg);
    }
}
