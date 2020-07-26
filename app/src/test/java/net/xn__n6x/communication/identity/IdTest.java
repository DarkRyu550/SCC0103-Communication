package net.xn__n6x.communication.identity;

import android.os.Parcel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

class IdTest {
    @Test
    public void buffer() {
        Id id = Id.fromString(getTestIdString());

        ByteBuffer buffer = ByteBuffer.allocate(Id.ID_LENGTH);
        id.intoBuffer(buffer);
        buffer.position(0);

        Id rec = Id.fromBuffer(buffer);
        Assertions.assertEquals(id, rec);
    }

    @Test
    public void string() {
        Id a = Id.fromString(getTestIdString());
        Id b = Id.fromString(a.toString());

        Assertions.assertEquals(a, b);
    }

    @Test
    public void invalidString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> { Id.fromString("0000"); });
        Assertions.assertThrows(IllegalArgumentException.class, () -> { Id.fromString("nope"); });
    }

    @Test
    public void testEquals() {
        Id a = Id.fromString(getTestIdString());
        Id b = Id.fromString(getTestIdString());
        Assertions.assertEquals(a, b);
    }

    @Test
    public void parcel() {
        Id a = Id.fromString(getTestIdString());
        Assertions.assertEquals(a.describeContents(), 0);
        Assertions.assertDoesNotThrow(() -> Id.CREATOR.newArray(0));
        /* Actually testing Parcelable does not work outside Android. */
    }

    @Test
    public void hashEquals() {
        Id a = Id.fromString(getTestIdString());
        Id b = Id.fromString(getTestIdString());

        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void randomDoesNotThrow() {
        Assertions.assertDoesNotThrow(Id::random);
    }

    @Test
    void testFromString() {
        Assertions.assertDoesNotThrow(() -> {
            Id a = Id.fromString(getTestIdString());
            Assertions.assertArrayEquals(a.data, getTestIdData());
        });
    }

    String getTestIdString() {
        StringBuilder idb = new StringBuilder();
        for(int i = 0; i < Id.ID_LENGTH; ++i)
            idb.append("69");
        return idb.toString();
    }

    byte[] getTestIdData() {
        byte[] data = new byte[Id.ID_LENGTH];
        for(int i = 0; i < Id.ID_LENGTH; ++i)
            data[i] = 0x69;
        return data;
    }
}