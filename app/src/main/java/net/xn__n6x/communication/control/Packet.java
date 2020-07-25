package net.xn__n6x.communication.control;

import net.xn__n6x.communication.identity.Id;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A data transmission packet. */
public class Packet {
    /** The ID of the device this packet originated from. */
    protected final Id source;
    /** The IDs of the devices this packet has already visited, in order. */
    protected final Id[] route;
    /* The ID of the device this packet is intended for. */
    protected final Id target;
    /** Payload data carried by this packet. */
    protected final byte[] payload;

    public Packet(Id source, Id[] route, Id target, byte[] payload) {
        this.source = source;
        this.route = route;
        this.target = target;
        this.payload = payload;
    }

    /** Read the first packet from the given byte buffer.
     * @param data The byte buffer containing the packet data.
     * @return The packet that has been read.
     */
    public static Packet fromBuffer(ByteBuffer data) {
        Id source = Id.fromBuffer(data);
        Id target = Id.fromBuffer(data);

        int routeLength = data.getInt();
        if(routeLength < 0)
            throw new IllegalArgumentException("Given buffer has packet with negative route length.");

        Id[] route = new Id[routeLength];
        for(int i = 0; i < routeLength; ++i)
            route[i] = Id.fromBuffer(data);

        int payloadLength = data.getInt();
        if(payloadLength < 0)
            throw new IllegalArgumentException("Given buffer has packet with negative payload length.");

        byte[] payload = new byte[payloadLength];
        data.get(payload);

        return new Packet(source, route, target, payload);
    }

    /** Returns a new {@link Packet} with the given {@link Id} tagged to the
     * end of the route this packet had to take. Use this if you are forwarding
     * the data in this packet to other devices. */
    public Packet tag(Id id) {
        ArrayList<Id> route = Stream.concat(Arrays.stream(this.route), Stream.of(id))
            .collect(Collectors.toCollection(ArrayList::new));

        /* please just lemme finish this */
        return new Packet(this.source, route.toArray(new Id[] {}), this.target, this.payload);
    }

    public void intoBuffer(ByteBuffer data) {
        this.source.intoBuffer(data);
        this.target.intoBuffer(data);

        data.putInt(this.route.length);
        for(Id id : this.route) id.intoBuffer(data);

        data.putInt(this.payload.length);
        data.put(this.payload);
    }

    public int intoBufferLength() {
        return (2 + this.route.length) * Id.ID_LENGTH   /* 2 plus route Ids.    */
            + 2 * 4                                     /* Two length values.   */
            + this.payload.length;                      /* And the payload.     */
    }

    public Id getSource() {
        return source;
    }

    public Id getTarget() {
        return target;
    }

    public byte[] getPayload() {
        return payload;
    }
}
