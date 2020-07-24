package net.xn__n6x.communication.identity;

public class Identity {
    /** The unique user identification value associated with this identity. */
    protected final Id identification;
    /** Human-readable information of whoever has this identity. */
    protected final Profile profile;

    public Identity(Id identification, Profile profile) {
        this.identification = identification;
        this.profile = profile;
    }
}
