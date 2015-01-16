package org.pcj.internal;

import org.pcj.NodeFailureException;
import org.pcj.internal.message.MessagePing;
import org.pcj.internal.utils.Configuration;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.synchronizedSet;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 11/20/14
 * Time: 9:46 PM
 * <p>
 * Monitors physical nodes of given group for network failure.
 * Informs registered callbacks in case of failure.
 */
public class NodesActivityMonitor implements Runnable {

    private final Thread monitoringThread = new Thread(this);

    private final MessagePing pingMessage;
    private boolean running = false;
    private final Set<Integer> skippedNodes;           // set of physical ids to monitor
    private final Set<Integer> nodesToMonitor;
    private final Map<Integer, Integer> nodesMap;
    private final Map<Integer, Long> responseTimes;
    private NodeFailureCallback callback = null;
    private long startTime;

    private Map<Integer, Exception> exceptionsByPhysicalNodes = new HashMap<>();

    public NodesActivityMonitor(int groupId, Map<Integer, Integer> nodes) { // global ids -> physical ids
        pingMessage = new MessagePing(groupId);
        this.nodesMap = nodes;
        this.skippedNodes = synchronizedSet(new HashSet<>());
        nodesToMonitor = new HashSet<>();
        monitoringThread.setDaemon(true);
        responseTimes = new ConcurrentHashMap<>();
    }

    public synchronized void start(NodeFailureCallback callback) {
        System.out.println("NODES MAP: " + nodesMap);
        if (running) {
            throw new IllegalStateException("Activity monitor already running");
        }
        nodesToMonitor.clear();
        nodesToMonitor.addAll(nodesMap.values());

        if (noNodesToMonitor()) {
            System.out.println("no nodes to monitor");
            return;
        }
        this.callback = callback;
        running = true;
        monitoringThread.start();
        startTime = currentTimeMillis();
    }

    private synchronized boolean noNodesToMonitor() {
        Set<Integer> nodes = new HashSet<>();
        nodes.addAll(nodesToMonitor);
        nodes.removeAll(skippedNodes);
        return nodes.isEmpty();
    }

    public synchronized void stop() {
        if (running) {
            monitoringThread.interrupt();
        }
    }

    public synchronized void markFinished(Integer nodeId) {
        skippedNodes.add(nodeId);
        if (skippedNodes.size() < nodesToMonitor.size()) {
            stop();
            if (running && !exceptionsByPhysicalNodes.isEmpty()) {
                System.out.println("should call finished with erors");
                callback.finishedWithErrors();
            }

        }
    }

    @Deprecated // not to be invoked manually
    public void run() {
        System.out.println("Started monitoring");
        while (true) {
            try {
                Thread.sleep(Configuration.NODE_PING_INTERVAL);
            } catch (InterruptedException e) {
                return;
            }
            boolean monitored = false;
            for (Integer nodeId : nodesToMonitor) { // nodes to monitor collection is set up once in start and should not be modified during run
                if (!skippedNodes.contains(nodeId)) { // in case the node is already removed from the nodes we wait for
                    monitored = true;
                    boolean success = false;
                    Exception exception = null;
                    try {
                        InternalPCJ.getNetworker().sendToPhysicalNode(nodeId, pingMessage);
                        System.out.println("sent ping to node: " + nodeId);
                        success = true;
                    } catch (IOException e) {
                        exception = e;
                    }
                    if (getResponseTime(nodeId) < currentTimeMillis() - Configuration.NODE_PING_TIMEOUT * 1000l) {
                        System.out.println("TIMEOUT!!!");
                        success = false;
                        exception = new RuntimeException("Node timeout"); // mstodo: fix it
                    }
                    if (!success) {
                        if (nodesToMonitor.contains(nodeId)) { // it might happen that by that time the node has finished
                            System.out.println("node failed!");
                            exceptionsByPhysicalNodes.put(nodeId, exception);
                            callback.error(nodeId, exception); // physical id !
                            markFinished(nodeId);
                        }
                    }
                }
            }
            if (!monitored) {
                return; // no nodes left for monitoring - all are in skippedNodes
            }
        }
    }

    private long getResponseTime(Integer nodeId) {
        Long pongTime = responseTimes.get(nodeId);
        return pongTime == null ? startTime : pongTime;
    }

    public void setResponseTime(int physicalNodeId) {
        System.out.println("pong");
        responseTimes.put(physicalNodeId, currentTimeMillis());
    }

    public Map<Integer, Exception> getExceptionsByPhysicalNodes() {
        return exceptionsByPhysicalNodes;
    }

    public void reset() {
        this.nodesToMonitor.clear();
        this.nodesToMonitor.addAll(nodesMap.values());
    }

    public static interface NodeFailureCallback {
        void error(int nodeId, Exception exception);
        void finishedWithErrors();
    }
}
