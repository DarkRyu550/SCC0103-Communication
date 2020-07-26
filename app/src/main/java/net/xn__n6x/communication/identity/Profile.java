package net.xn__n6x.communication.identity;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Profile profile = (Profile) o;
        return Objects.equals(name, profile.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
