package net.xn__n6x.communication.identity;

import java.util.Objects;

public class Identity {
    /** The unique user identification value associated with this identity. */
    protected final Id identification;
    /** Human-readable information of whoever has this identity. */
    protected final Profile profile;

    public Identity(Id identification, Profile profile) {
        this.identification = identification;
        this.profile = profile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity identity = (Identity) o;
        return Objects.equals(identification, identity.identification) &&
            Objects.equals(profile, identity.profile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identification, profile);
    }
}
