/*
 * Copyright (c) 2011-2016, PCJ Library, Marek Nowicki
 * All rights reserved.
 *
 * Licensed under New BSD License (3-clause license).
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.internal.futures;

import org.pcj.PCJ;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.Bitmask;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class BroadcastState extends InternalFuture<Void> implements PcjFuture<Void> {

    private final Queue<Exception> exceptions;
    private final Bitmask physicalBitmask;
    private final Bitmask physicalMaskBitmask;
    private Exception exception;

    public BroadcastState(Bitmask physicalBitmask) {
        System.out.println("Initializing broadcast state, " + physicalBitmask);
        this.physicalBitmask = new Bitmask(physicalBitmask.getSize());
        physicalMaskBitmask = new Bitmask(physicalBitmask);

        this.exceptions = new ConcurrentLinkedDeque<>();
    }

    public void addException(Exception ex) {
        exceptions.add(ex);
    }

    public void signalException(Exception exception) {
        this.exception = exception;
        super.signalDone();
    }

    @Override
    public void signalDone() {
        System.out.println("[" + PCJ.getNodeId() + "] will signal done");
        super.signalDone();
        System.out.println("[" + PCJ.getNodeId() + "] signaled done");
    }

    @Override
    public boolean isDone() {
        return super.isSignaled();
    }

    @Override
    public Void get() throws PcjRuntimeException {
        try {
            super.await();
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            throw new PcjRuntimeException(exception);
        }
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException {
        try {
            super.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            throw new PcjRuntimeException(exception);
        }
        return null;
    }

    private void setPhysical(int physicalId) {
        physicalBitmask.set(physicalId);
    }

    private boolean isPhysicalSet() {
        return physicalBitmask.isSet(physicalMaskBitmask);
    }

    public synchronized void processPhysical(int physicalId) {
        this.setPhysical(physicalId);
        System.out.println("[" + PCJ.getNodeId() + "] broadcast state setting physical: " + physicalId + ", physicalset: " + isPhysicalSet());// mstodo remove
        if (isPhysicalSet()) {
            System.out.println("[" + PCJ.getNodeId() + "] physical set, will signal done");// mstodo remove
            if (!isExceptionOccurs()) {
                signalDone();
            } else {
                RuntimeException ex = new RuntimeException("Exception while broadcasting value.");
                getExceptions().forEach(ex::addSuppressed);

                System.out.println("[" + PCJ.getNodeId() + "] done with exception");// mstodo remove
                signalException(ex);
            }
        } else {
            System.out.println("[" + PCJ.getNodeId() + "] still not done, ");// mstodo remove
        }
        System.out.println("[" + PCJ.getNodeId() + "] state: " + physicalBitmask + ", mask: " + physicalMaskBitmask
                + " is done: " + isPhysicalSet() + ", is exception: " + isExceptionOccurs());
    }

    public boolean isExceptionOccurs() {
        return !exceptions.isEmpty();
    }

    public Queue<Exception> getExceptions() {
        return exceptions;
    }
}
