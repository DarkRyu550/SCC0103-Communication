package net.xn__n6x.communication;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AssertionsTest {
    @Test
    void debugAssert() {
        if(BuildConfig.DEBUG) {
            Assertions.assertDoesNotThrow(() -> net.xn__n6x.communication.Assertions.debugAssert(true));
            Assertions.assertThrows(AssertionError.class, () -> net.xn__n6x.communication.Assertions.debugAssert(false));
        } else {
            Assertions.assertDoesNotThrow(() -> net.xn__n6x.communication.Assertions.debugAssert(true));
            Assertions.assertDoesNotThrow(() -> net.xn__n6x.communication.Assertions.debugAssert(false));
        }
    }

    @Test
    void debugAssertEquals() {
        if(BuildConfig.DEBUG) {
            Assertions.assertDoesNotThrow(() -> net.xn__n6x.communication.Assertions.debugAssertEquals(1, 1));
            Assertions.assertThrows(AssertionError.class, () -> net.xn__n6x.communication.Assertions.debugAssertEquals(1, 0));
        } else {
            Assertions.assertDoesNotThrow(() -> net.xn__n6x.communication.Assertions.debugAssertEquals(1, 1));
            Assertions.assertDoesNotThrow(() -> net.xn__n6x.communication.Assertions.debugAssertEquals(1, 0));
        }
    }

    @Test
    void fail() {
        Assertions.assertThrows(AssertionError.class, () -> net.xn__n6x.communication.Assertions.fail("test"));
    }
}