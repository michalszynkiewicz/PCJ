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
import org.pcj.internal.ft.NodeFailedException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class GetVariable<T> extends InternalFuture<T> implements PcjFuture<T> {

    private T variableValue;
    private Integer threadId;
    private Exception exception;

    public GetVariable(int threadId) {
        this.threadId = threadId;
    }

    @SuppressWarnings("unchecked")
    public void signalDone(Object variableValue) {
        this.variableValue = (T) variableValue;
        super.signalDone();
    }

    public void signalException(Exception exception) {
        this.exception = exception;
        super.signalDone();
    }

    public void setThreadId(Integer threadId) {
        this.threadId = threadId;
    }

    public Integer getThreadId() {
        return threadId;
    }

    @Override
    public boolean isDone() {
        return super.isSignaled();
    }

    @Override
    public T get() throws PcjRuntimeException {
        try {
            System.out.println("[" + PCJ.getNodeId() + "] GVar will wait from " + threadId);
            super.await();
            System.out.println("[" + PCJ.getNodeId() + "] GVar finished from " + threadId);
        } catch (InterruptedException ex) {
            System.out.println("[" + PCJ.getNodeId() + "] GVar interrupted");
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            if (exception instanceof NodeFailedException) {
                throw (NodeFailedException) exception;
            } else {
                throw new PcjRuntimeException(exception);
            }
        }
        return variableValue;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException {
        try {
            super.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        if (exception != null) {
            if (exception instanceof NodeFailedException) {
                throw (NodeFailedException) exception;
            } else {
                throw new PcjRuntimeException(exception);
            }
        }
        return variableValue;
    }
}
