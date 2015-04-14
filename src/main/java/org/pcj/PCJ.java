/*
 * This file is the part of the PCJ Library
 */
package org.pcj;

import org.pcj.internal.InternalPCJ;
import org.pcj.internal.InternalStartPoint;
import org.pcj.internal.PcjThread;
import org.pcj.internal.faulttolerance.Lock;
import org.pcj.internal.storage.InternalStorage;
import org.pcj.internal.utils.Configuration;
import org.pcj.internal.utils.NodesFile;

import java.io.IOException;

/**
 * Main PCJ class with static methods.
 * <p>
 * Static methods provide way to use library.
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */

// todo: fix locking
final public class PCJ extends org.pcj.internal.InternalPCJ {

    // Suppress default constructor for noninstantiability
    private PCJ() {
        throw new AssertionError();
    }

    /**
     * Deploys and starts PCJ calculations on nodes using specified StartPoint
     * and Storage class. Array <tt>nodes</tt> contains list of hostnames.
     * Hostnames can be specified many times, so more than one instance of PCJ
     * will be run on node. Empty hostnames means current JVM.
     * <p>
     * Hostnames can take port (after colon ':'), eg. ["localhost:8000",
     * "localhost:8001", "localhost", "host2:8001", "host2"]. Default port is
     * 8091 and can be modified using <tt>pcj.port</tt> system property value.
     *
     * @param startPoint start point class
     * @param storage    storage class
     * @param nodes      array of nodes
     */
    public static void deploy(Class<? extends InternalStartPoint> startPoint,
                              Class<? extends InternalStorage> storage,
                              String[] nodes) {
        NodesFile nodesFile = new NodesFile(nodes);
        InternalPCJ.deploy(startPoint, storage, nodesFile);
    }

    /**
     * Deploys and starts PCJ calculations on nodes using specified StartPoint
     * and Storage class.
     *
     * @param startPoint    start point class
     * @param storage       storage class
     * @param nodesFilename file with descriptions of nodes
     */
    public static void deploy(Class<? extends InternalStartPoint> startPoint,
                              Class<? extends InternalStorage> storage,
                              String nodesFilename) {
        try {
            NodesFile nodesFile = new NodesFile(nodesFilename);
            InternalPCJ.deploy(startPoint, storage, nodesFile);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Deploys and starts PCJ calculations on nodes using specified StartPoint
     * and Storage class. Descriptions of nodes are read from default nodefile
     * according to the system settings:
     * <ol><li>pcj.nodefile - JVM property</li>
     * <li>NODEFILE - system property</li>
     * <li>LOADL_HOSTFILE - system property</li>
     * <li>PBS_NODEFILE - system property</li>
     * <li>or <i>"nodes.file"</i></li></ol>
     *
     * @param startPoint start point class
     * @param storage    storage class
     */
    public static void deploy(Class<? extends InternalStartPoint> startPoint,
                              Class<? extends InternalStorage> storage) {
        deploy(startPoint, storage, Configuration.NODES_FILENAME);
    }

    /**
     * Starts PCJ calculations on local node using specified StartPoint and
     * Storage class. Array <tt>nodes</tt> contains list of all hostnames used
     * in calculations.
     *
     * @param startPoint start point class
     * @param storage    storage class
     * @param nodes      array of nodes
     */
    public static void start(Class<? extends InternalStartPoint> startPoint,
                             Class<? extends InternalStorage> storage,
                             String[] nodes) {
        NodesFile nodesFile = new NodesFile(nodes);
        InternalPCJ.start(startPoint, storage, nodesFile);
    }

    /**
     * Starts PCJ calculations on local node using specified StartPoint and
     * Storage class. Descriptions of all nodes used in calculations are read
     * from supplied file.
     *
     * @param startPoint    start point class
     * @param storage       storage class
     * @param nodesFilename file with descriptions of nodes
     */
    public static void start(Class<? extends InternalStartPoint> startPoint,
                             Class<? extends InternalStorage> storage,
                             String nodesFilename) {
        try {
            NodesFile nodesFile = new NodesFile(nodesFilename);
            InternalPCJ.start(startPoint, storage, nodesFile);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Starts PCJ calculations on local node using specified StartPoint and
     * Storage class. Descriptions of all nodes used in calculations are read
     * from default nodefile according to the system settings:
     * <ol><li>pcj.nodefile - JVM property</li>
     * <li>NODEFILE - system property</li>
     * <li>LOADL_HOSTFILE - system property</li>
     * <li>PBS_NODEFILE - system property</li>
     * <li>or <i>"nodes.file"</i></li></ol>
     *
     * @param startPoint start point class
     * @param storage    storage class
     */
    public static void start(Class<? extends InternalStartPoint> startPoint,
                             Class<? extends InternalStorage> storage) {
        start(startPoint, storage, Configuration.NODES_FILENAME);
    }

//    @SuppressWarnings("unchecked")
//    public static <X extends Storage> X getStorage() {
//        return (X) PcjThread.threadStorage();
//    }

    /**
     * Returns global node id.
     *
     * @return global node id
     */
    public static int myId() {
        Lock.readLock();
        try {
            return ((Group) PcjThread.threadGlobalGroup()).myId();
        } finally

        {
            Lock.readUnlock();
        }
    }

    /**
     * Returns physical node id (internal value for distinguishing nodes).
     *
     * @return physical node id
     */
    public static int getPhysicalNodeId() {
        Lock.readLock();
        try {
            return InternalPCJ.getPhysicalNodeId();
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Returns global number of nodes used in calculations.
     *
     * @return global number of nodes used in calculations
     */
    public static int threadCount() {
        Lock.readLock();
        try {
            return ((Group) PcjThread.threadGlobalGroup()).threadCount();
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Synchronizes all nodes used in calculations.
     */
    public static void barrier() {
        Lock.readLock();
        try {
            ((Group) PcjThread.threadGlobalGroup()).barrier();
        } finally {
            Lock.readUnlock();
        }
    }

    public static void barrier(int node) {
        Lock.readLock();
        try {
            ((Group) PcjThread.threadGlobalGroup()).barrier(node);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Resets the monitoring state.
     *
     * @param variable name of variable
     */
    public static void monitor(String variable) {
        Lock.readLock();
        try {
            PcjThread.threadStorage().monitor(variable);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Causes the current thread to wait until the variable was <i>touched</i>.
     * Resets the state after <i>touch</i>. The waitFor(variable) method has the
     * same effect as:
     * <pre><code>waitFor(variable, 1)</code></pre>
     *
     * @param variable name of variable
     */
    public static void waitFor(String variable) {
        Lock.readLock();
        try {
            PcjThread.threadStorage().waitFor(variable);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Causes the current thread to wait until the variable was <i>touched</i>
     * count times. Resets the state after <i>touches</i>.
     *
     * @param variable name of variable
     * @param count    number of <i>touches</i>
     */
    public static void waitFor(String variable, int count) {
        Lock.readLock();
        try {
            PcjThread.threadStorage().waitFor(variable, count);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Gets the value from current thread Storage.
     *
     * @param variable name of variable
     * @return value of variable
     */
    public static <T> T getLocal(String variable) {
        Lock.readLock();
        try {
            return PcjThread.threadStorage().get(variable);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Gets the value from current thread Storage
     *
     * @param variable name of array variable
     * @param indexes  indexes of array
     * @return value of variable
     */
    public static <T> T getLocal(String variable, int... indexes) {
        Lock.readLock();
        try {
            return PcjThread.threadStorage().get(variable, indexes);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Fully asynchronous get from other thread Storage
     *
     * @param nodeId   global node id
     * @param variable name of array variable
     * @return FutureObject that will contain received data
     */
    public static <T> FutureObject<T> getFutureObject(int nodeId, String variable) {
        Lock.readLock();
        try {
            return ((Group) PcjThread.threadGlobalGroup()).getFutureObject(nodeId, variable);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Fully asynchronous get from other thread Storage
     *
     * @param nodeId   global node id
     * @param variable name of array variable
     * @param indexes  indexes of array
     * @return FutureObject that will contain received data
     */
    public static <T> FutureObject<T> getFutureObject(int nodeId, String variable, int... indexes) {
        Lock.readLock();
        try {
            return ((Group) PcjThread.threadGlobalGroup()).getFutureObject(nodeId, variable, indexes);
        } finally {
            Lock.readUnlock();
        }
    }

    public static <T> T get(int nodeId, String variable) {
        Lock.readLock();
        FutureObject<T> futureObject;
        try {
            futureObject = getFutureObject(nodeId, variable);
        } finally {
            Lock.readUnlock();
        }
        return futureObject.get();
    }

    public static <T> T get(int nodeId, String variable, int... indexes) {
        Lock.readLock();
        FutureObject<T> futureObject;
        try {
            futureObject = getFutureObject(nodeId, variable, indexes);
        } finally {
            Lock.readUnlock();
        }
        return futureObject.get();

    }

    /**
     * Puts the value to current thread Storage
     *
     * @param variable name of variable
     * @param newValue new value of variable
     * @throws ClassCastException when the value cannot be cast to the type of
     *                            variable in Storage
     */
    public static void putLocal(String variable, Object newValue) throws ClassCastException {
        Lock.readLock();
        try {
            PcjThread.threadStorage().put(variable, newValue);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Puts the value to current thread Storage
     *
     * @param variable name of array variable
     * @param newValue new value of variable
     * @param indexes  indexes of array
     * @throws ClassCastException when the value cannot be cast to the type of
     *                            variable in Storage
     */
    public static void putLocal(String variable, Object newValue, int... indexes) throws ClassCastException {
        Lock.readLock();
        try {
            PcjThread.threadStorage().put(variable, newValue, indexes);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Puts the value to other thread Storage
     *
     * @param nodeId   other node global node id
     * @param variable name of variable
     * @param newValue new value of variable
     * @throws ClassCastException when the value cannot be cast to the type of
     *                            variable in Storage
     */
    public static <T> void put(int nodeId, String variable, T newValue) throws ClassCastException {
        Lock.readLock();
        try {
            if (PcjThread.threadStorage().isAssignable(variable, newValue) == false) {
                throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
                        + " to the type of variable '" + variable + "'");
            }
            ((Group) PcjThread.threadGlobalGroup()).put(nodeId, variable, newValue);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Puts the value to other thread Storage
     *
     * @param nodeId   other node global node id
     * @param variable name of array variable
     * @param newValue new value of variable
     * @param indexes  indexes of array
     * @throws ClassCastException when the value cannot be cast to the type of
     *                            variable in Storage
     */
    public static <T> void put(int nodeId, String variable, T newValue, int... indexes) throws ClassCastException {
        Lock.readLock();
        try {
            if (PcjThread.threadStorage().isAssignable(variable, newValue, indexes) == false) {
                throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
                        + " to the type of variable '" + variable + "'");
            }
            ((Group) PcjThread.threadGlobalGroup()).put(nodeId, variable, newValue, indexes);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Broadcast the value to all threads and inserts it into Storage
     *
     * @param variable name of variable
     * @param newValue new value of variable
     * @throws ClassCastException when the value cannot be cast to the type of
     *                            variable in Storage
     */
    public static void broadcast(String variable, Object newValue) throws ClassCastException {
        Lock.readLock();
        try {
            if (PcjThread.threadStorage().isAssignable(variable, newValue) == false) {
                throw new ClassCastException("Cannot cast " + newValue.getClass().getCanonicalName()
                        + " to the type of variable '" + variable + "'");
            }
            ((Group) PcjThread.threadGlobalGroup()).broadcast(variable, newValue);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Returns the global group
     *
     * @return the global group
     */
    public static Group getGlobalGroup() {
        Lock.readLock();
        try {
            return ((Group) PcjThread.threadGlobalGroup());
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Returns group by name
     *
     * @param name name of the group
     * @return group by name
     */
    public static Group getGroup(String name) {
        Lock.readLock();
        try {
            return (Group) PcjThread.threadGroup(name);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Joins the current thread to the group
     *
     * @param name name of the group
     * @return group to which thread joined
     */
    public static Group join(String name) {
        Lock.readLock();
        try {
            int myNodeId = ((Group) PcjThread.threadGlobalGroup()).myId();
            return (Group) InternalPCJ.join(myNodeId, name);
        } finally {
            Lock.readUnlock();
        }
    }

    /**
     * Sends message with log message
     *
     * @param text text to send
     */
    public static void log(String text) {
        Lock.readLock();
        try {
            ((Group) PcjThread.threadGlobalGroup()).log(text);
        } finally {
            Lock.readUnlock();
        }
    }
}
