package net.xn__n6x.communication.identity;

public class Profile {
    /** Display name. This name will be the one other users will see when they
     * contact you or look at your profile. */
    protected final String name;

    /** Create a new profile object. */
    public Profile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
