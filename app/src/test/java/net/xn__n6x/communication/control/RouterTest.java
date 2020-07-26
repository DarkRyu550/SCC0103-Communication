package net.xn__n6x.communication.control;

import net.xn__n6x.communication.identity.Id;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest {

    @Test
    void getNextMessageForPeer() {
        Id self = Id.random();
        Router r = new Router(self);

        Id other = Id.random();
        r.register(other);

        /* Create an incoming packet from peer and send it. */
        Packet p = new Packet(other, new Id[] {}, self, new byte[] {});
        r.forward(p, Router.DEFAULT_TIME_TO_LIVE);

        /* Retrieve the packet and make sure it's the same as the one we'd sent. */
        Packet q = r.getNextMessageForPeer(other)
            .orElseThrow(() -> new AssertionError("Expect next message"));
        Assertions.assertEquals(p, q);
    }

    @Test
    void getTargetedReachablePeersDirect() {
        Id self = Id.random();
        Router r = new Router(self);

        Id otherA = Id.random();
        Id otherB = Id.random();
        Id otherC = Id.random();
        r.register(otherA);
        r.register(otherB);
        r.register(otherC);

        /* Create a packet to otherA and send it. */
        Packet p = new Packet(self, new Id[] {}, otherA, new byte[] {});
        r.forward(p, Router.DEFAULT_TIME_TO_LIVE);

        /* Make sure only otherA is marked as targetedReachable. */
        HashSet<Id> ids = r.getTargetedReachablePeers();
        Assertions.assertEquals(ids.size(), 1);
        Assertions.assertEquals(ids.iterator().next(), otherA);
    }

    @Test
    void getTargetedReachablePeersIndirect() {
        Id self = Id.random();
        Router r = new Router(self);

        /* Only register A and B as reachable. */
        Id otherA = Id.random();
        Id otherB = Id.random();
        Id otherC = Id.random();
        r.register(otherA);
        r.register(otherB);

        /* Create packet to otherC and send it. */
        Packet p = new Packet(self, new Id[] {}, otherC, new byte[] {});
        r.forward(p, Router.DEFAULT_TIME_TO_LIVE);

        /* Make sure A and B are marked as targetedReachable. */
        HashSet<Id> ids = r.getTargetedReachablePeers();
        Assertions.assertEquals(ids.size(), 2);
        Assertions.assertTrue(ids.contains(otherA));
        Assertions.assertTrue(ids.contains(otherB));
    }

    @Test
    void getNextMessageForPeerWhenPeerIsUnknown() {
        Id self = Id.random();
        Router r = new Router(self);

        Id other = Id.random();
        Assertions.assertFalse(r.getNextMessageForPeer(other).isPresent());
    }

    @Test
    void getNextMessageForPeerWhenThereAreNone() {
        Id self = Id.random();
        Router r = new Router(self);

        Id other = Id.random();
        r.register(other);

        Assertions.assertFalse(r.getNextMessageForPeer(other).isPresent());
    }

    @Test
    void retain() {
        Id self = Id.random();
        Router r = new Router(self);

        Id otherA = Id.random();
        Id otherB = Id.random();
        Id otherC = Id.random();
        r.register(otherA);
        r.register(otherB);
        r.register(otherC);

        HashSet<Id> reachable = new HashSet<>(3);
        reachable.add(otherA);
        reachable.add(otherC);

        r.retain(reachable);
        Assertions.assertEquals(reachable, r.getReachablePeers());
    }

    @Test
    void getReachablePeers() {
        Id self = Id.random();
        Router r = new Router(self);

        Id otherA = Id.random();
        Id otherB = Id.random();
        Id otherC = Id.random();
        r.register(otherA);
        r.register(otherB);
        r.register(otherC);

        HashSet<Id> reachable = new HashSet<>(3);
        reachable.add(otherA);
        reachable.add(otherB);
        reachable.add(otherC);

        Assertions.assertEquals(reachable, r.getReachablePeers());
    }
}