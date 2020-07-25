package net.xn__n6x.communication.identity;

import android.os.Parcel;
import android.os.Parcelable;
import net.xn__n6x.communication.Assertions;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** The ID of a given phone. */
public class Id implements Parcelable {
    /** How many bytes the internal data array will store. */
    public static final int ID_LENGTH = 64;

    /* Printable format useful for formatting our data parameter. */
    protected static final String ID_FORMAT =
        IntStream.range(0, ID_LENGTH)
            .mapToObj(i -> "%02x")
            .reduce("", (a, x) -> a + x);

    /** The actual data for the identification value, represented as a 32-byte
     * unsigned little endian integer. */
    protected final byte[] data;

    /** Creates an ID tag object from its data.
     * @param data The identification data.
     */
    protected Id(byte[] data) {
        Assertions.debugAssertEquals(data.length, ID_LENGTH);
        this.data = data;
    }

    /** Creates a new random {@link net.xn__n6x.communication.identity.Id} from
     * high quality randomness source. The high quality source that will be used
     * is from {@link java.security.SecureRandom}, using its default provider.
     * @return A randomly generated {@link Id}.
     */
    public static Id random() {
        SecureRandom random = new SecureRandom();

        byte[] data = new byte[ID_LENGTH];
        random.nextBytes(data);

        return new Id(data);
    }

    /** Reads an Id from the given {@link java.nio.ByteBuffer} object.
     * @param buffer The buffer from which the {@link Id} will be read.
     * @return The {@link Id} that has been read from it.
     */
    public static Id fromBuffer(ByteBuffer buffer) {
        byte[] bytes = new byte[Id.ID_LENGTH];
        buffer.get(bytes);

        return new Id(bytes);
    }

    public void intoBuffer(ByteBuffer buffer) {
        buffer.put(this.data);
    }

    /** Tries to parse a string into an {@link Id}.
     * @param source The {@link String} to be parsed.
     * @return The {@link Id} parsed from the given string.
     * @throws IllegalArgumentException When the given string does not contain
     * a valid {@link Id} in string format.
     */
    public static Id fromString(String source) {
        try {
            Byte[] bytes = IntStream.range(0, source.length() / 2)
                .mapToObj(i -> source.substring(i * 2, i * 2 + 2))
                .map(b -> (byte)Integer.parseInt(b, 16))
                .toArray(Byte[]::new);

            if(bytes.length != ID_LENGTH)
                throw new IllegalArgumentException(
                    "Expected to extract " + ID_LENGTH + " bytes, got " + bytes.length + "instead"
                );

            byte[] target = new byte[bytes.length];
            for(int i = 0; i < target.length; ++i)
                target[i] = bytes[i];

            return new Id(target);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Invalid character sequence", e);
        }
    }

    @Override
    public String toString() {
        Byte[] bytes = new Byte[this.data.length];
        Arrays.setAll(bytes, i -> this.data[i]);

        /* I miss String#repeat(). */
        return String.format(ID_FORMAT, (Object[]) bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Id id = (Id) o;
        return Arrays.equals(data, id.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    /* Parcelable boilerplate from here on out. */
    @Override
    public int describeContents() {
        /* We are saving no file descriptors. */
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.data);
    }

    public static final Parcelable.Creator<Id> CREATOR = new Creator<Id>() {
        @Override
        public Id createFromParcel(Parcel source) {
            byte[] data = new byte[Id.ID_LENGTH];
            source.readByteArray(data);

            return new Id(data);
        }

        @Override
        public Id[] newArray(int size) {
            return new Id[size];
        }
    };

}
