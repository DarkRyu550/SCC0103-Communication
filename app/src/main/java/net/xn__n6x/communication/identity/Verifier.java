package net.xn__n6x.communication.identity;

/** Provides connection to a REST interface which verifies whether any given ID
 * has already been generated. Note that given the number of distinct values
 * that can be represented by {@link Id} is massive (See {@code Id.ID_LENGTH}
 * for the actual number), it should not be strictly necessary to verify this.
 * The possibility of a collision by random number generation is negligible.
 */
public class Verifier {
    private static final String PROVIDER = "https://xn--n6x.net/collider/";
}
