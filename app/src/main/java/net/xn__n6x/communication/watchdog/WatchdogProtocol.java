package net.xn__n6x.communication.watchdog;

import net.xn__n6x.communication.control.Packet;
import net.xn__n6x.communication.identity.Id;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public class WatchdogProtocol {
    public static final byte[] MAGIC = new byte[] { 0x00, 0x47, 0x61, 0x79, 0x36, 0x39 };

    protected Socket socket;
    public WatchdogProtocol(Socket socket) {
        this.socket = socket;
    }

    public void sendMagic() throws IOException {
        this.socket.getOutputStream().write(MAGIC);
    }

    public void sendId(Id id) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Id.ID_LENGTH);
        id.intoBuffer(buffer);

        this.socket.getOutputStream().write(buffer.array());
    }

    public void sendInt(int count) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(count);

        this.socket.getOutputStream().write(buffer.array());
    }

    public void sendString(String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);

        this.sendInt(bytes.length);
        this.socket.getOutputStream().write(bytes);
    }

    public void sendPacket(Packet p) throws IOException {
        this.sendInt(p.intoBufferLength());

        ByteBuffer buffer = ByteBuffer.allocate(p.intoBufferLength());
        p.intoBuffer(buffer);

        this.socket.getOutputStream().write(buffer.array());
    }

    public Optional<byte[]> getValidMagic() throws IOException {
        byte[] magic = new byte[MAGIC.length];
        if(this.socket.getInputStream().read(magic) != MAGIC.length)
            return Optional.empty();
        if(!Arrays.equals(MAGIC, magic))
            return Optional.empty();

        return Optional.of(magic);
    }

    public Optional<Id> getId() throws IOException {
        byte[] id = new byte[Id.ID_LENGTH];
        if(this.socket.getInputStream().read(id) != Id.ID_LENGTH)
            return Optional.empty();

        return Optional.of(Id.fromBuffer(ByteBuffer.wrap(id)));
    }

    public Optional<Integer> getInt() throws IOException {
        byte[] integer = new byte[4];
        if(this.socket.getInputStream().read(integer) != 4)
            return Optional.empty();

        return Optional.of(ByteBuffer.wrap(integer).getInt());
    }

    public Optional<String> getString() throws IOException {
        Optional<Integer> optLength = this.getInt();
        int length;
        if(optLength.isPresent())
            length = optLength.get();
        else
            return Optional.empty();

        byte[] bytes = new byte[length];
        if(this.socket.getInputStream().read(bytes) != length)
            return Optional.empty();

        return Optional.of(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString());
    }

    public Optional<Packet> getValidPacket() throws IOException {
        Optional<Integer> optLength = this.getInt();
        int length;
        if(optLength.isPresent())
            length = optLength.get();
        else
            return Optional.empty();

        byte[] packet = new byte[length];
        if(this.socket.getInputStream().read(packet) != length)
            return Optional.empty();

        return Optional.of(Packet.fromBuffer(ByteBuffer.wrap(packet)));
    }
}
