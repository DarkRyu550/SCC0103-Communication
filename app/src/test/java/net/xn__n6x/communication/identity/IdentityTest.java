package net.xn__n6x.communication.identity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IdentityTest {
    @Test
    void identity() {
        Id id = Id.random();

        String name = "Test";
        Profile p = new Profile(name);

        Identity identity = new Identity(id, p);
        Identity other = new Identity(id, p);
        Assertions.assertEquals(identity.identification, id);
        Assertions.assertEquals(identity.profile, p);

        Assertions.assertEquals(identity, other);
        Assertions.assertEquals(identity.hashCode(), other.hashCode());
    }

    @Test
    void profile() {
        String name = "Test";
        Profile p = new Profile(name);
        Profile q = new Profile(name);

        Assertions.assertEquals(name, p.name);
        Assertions.assertEquals(name, p.getName());
        Assertions.assertEquals(p, q);
        Assertions.assertEquals(p.hashCode(), q.hashCode());
    }
}