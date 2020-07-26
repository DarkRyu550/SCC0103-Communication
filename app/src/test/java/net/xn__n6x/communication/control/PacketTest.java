package net.xn__n6x.communication.control;

import net.xn__n6x.communication.identity.Id;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;

class PacketTest {
    @Test
    void buffer() {
        Id     source = Id.random();
        Id[]   route  = new Id[] { Id.random() };
        Id     target = Id.random();
        byte[] data   = new byte[] { 0x69 };

        Packet p = new Packet(source, route, target, data);

        ByteBuffer buffer = ByteBuffer.allocate(p.intoBufferLength());
        p.intoBuffer(buffer);
        buffer.position(0);

        Packet q = Packet.fromBuffer(buffer);
        Assertions.assertEquals(p, q);
    }

    @Test
    void invalidBuffer() {
        Id idA = Id.random();
        Id idB = Id.random();

        /* Craft an invalid buffer that will fail in the first check. */
        ByteBuffer failA = ByteBuffer.allocate(Id.ID_LENGTH * 2 + 4);
        idA.intoBuffer(failA);
        idB.intoBuffer(failA);
        failA.putInt(-1);
        failA.position(0);

        /* Craft an invalid buffer that will fail in the second check. */
        ByteBuffer failB = ByteBuffer.allocate(Id.ID_LENGTH * 2 + 8);
        idA.intoBuffer(failB);
        idB.intoBuffer(failB);
        failB.putInt(0);
        failB.putInt(-1);
        failB.position(0);

        /* Test both for failures. */
        Assertions.assertThrows(IllegalArgumentException.class, () -> Packet.fromBuffer(failA));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Packet.fromBuffer(failB));
    }



    @Test
    void support() {
        Id     source = Id.random();
        Id[]   route  = new Id[] { Id.random() };
        Id     target = Id.random();
        byte[] data   = new byte[] { 0x69 };

        Packet p = new Packet(source, route, target, data);
        Packet q = new Packet(source, route, target, data);

        Assertions.assertEquals(p.getSource(), source);
        Assertions.assertEquals(p.getTarget(), target);
        Assertions.assertArrayEquals(p.getRoute(), route);
        Assertions.assertArrayEquals(p.getPayload(), data);


        Assertions.assertEquals(p, q);
        Assertions.assertEquals(p.hashCode(), q.hashCode());
    }

    @Test
    void tag() {
        Id     source = Id.random();
        Id     interm = Id.random();
        Id[]   routeA = new Id[] { Id.random() };
        Id[]   routeB = new Id[] { routeA[0], interm };
        Id     target = Id.random();
        byte[] data   = new byte[] { 0x69 };

        Packet p = new Packet(source, routeA, target, data);
        Packet q = new Packet(source, routeB, target, data);

        Assertions.assertEquals(q, p.tag(interm));
    }
}