/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.internal.futures;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;
import org.pcj.internal.Bitmask;

/**
 *
 * @author faramir
 */
public class LocalBarrier extends InternalFuture<Void> implements PcjFuture<Void> {

    private final Bitmask localBarrierBitmask;
    private final Bitmask localBarrierMaskBitmask;

    public LocalBarrier(int round, Bitmask localBitmask) {
        this.localBarrierBitmask = new Bitmask(localBitmask.getSize());
        this.localBarrierMaskBitmask = new Bitmask(localBitmask);
    }

    public void set(int index) {
        localBarrierBitmask.set(index);
    }

    public boolean isSet() {
        return localBarrierBitmask.isSet(localBarrierMaskBitmask);
    }

    @Override
    public void signalAll() {
        super.signalAll();
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
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws TimeoutException, PcjRuntimeException {
        try {
            super.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw new PcjRuntimeException(ex);
        }
        return null;
    }
}