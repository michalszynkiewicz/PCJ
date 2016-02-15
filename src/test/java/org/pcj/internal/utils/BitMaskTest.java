package org.pcj.internal.utils;

import org.junit.Test;

public class BitMaskTest {

    /*mstodo add test to cover setting last when one is removed (do it on group/fail handler level)*/
    @Test
    public void shouldSucceedToRemoveIdx() {
        BitMask mask = new BitMask(5);
        BitMask newMask = mask.without(4);
        assert newMask.getSize() == 4;
    }

    @Test
    public void shouldMarkSetIfOnlyOneLeftIsRemoved() {
        BitMask mask = new BitMask(4);
        mask.set(0);
        mask.set(2);
        mask.set(3);
        assert mask.without(1).isSet();
    }

    @Test
    public void shouldMarkSetIfOnlyTwoLeftAreRemoved() {
        BitMask mask = new BitMask(5);
        mask.set(0);
        mask.set(2);
        mask.set(3);
        mask = mask.without(4);
        assert mask.without(1).isSet();
    }

    @Test
    public void shouldMarkSetIfOnlyTwoLeftAreRemovedWithRenumbering() {
        BitMask mask = new BitMask(5);
        mask.set(0);
        mask.set(2);
        mask.set(3);
        mask = mask.without(1);
        assert mask.without(3).isSet();
    }
}