package net.xn__n6x.communication;

/** Provides assertions in order to guarantee code invariants are respected. */
public final class Assertions {
    /** If this project is in debug mode, throws an exception when the predicate
     * is equal to false. Does nothing if the project is not is debug mode.
     * @param val The predicate.
     */
    public static void debugAssert(boolean val) {
        if(BuildConfig.DEBUG && !val)
            throw new AssertionError("Precondition failed");
    }

    /** If this project is in debug mode, throws an exception when the result of
     * calling {@code lhs.equals(rhs)} is equal to false. Does nothing if the
     * project is not is debug mode.
     * @param lhs The value in the left hand side.
     * @param rhs The value in the right hand side.
     * @param <A> The type of the values being compared.
     */
    public static <A> void debugAssertEquals(A lhs, A rhs) {
        if(BuildConfig.DEBUG && !lhs.equals(rhs)) {
            String message = String.format(
                "Equality condition failed: (lhs != rhs) lhs <- %s, rhs <- %s",
                lhs, rhs);

            @SuppressWarnings("UnnecessaryLocalVariable")
            AssertionError up = new AssertionError(message);
            throw up;
        }
    }

    /** If this project is in debug mode, throws an exception when the result of
     * calling {@code lhs.equals(rhs)} is equal to true. Does nothing if the
     * project is not is debug mode.
     * @param lhs The value in the left hand side.
     * @param rhs The value in the right hand side.
     * @param <A> The type of the values being compared.
     */
    public static <A> void debugAssertDiffers(A lhs, A rhs) {
        if(BuildConfig.DEBUG && lhs.equals(rhs)) {
            String message = String.format(
                "Inequality condition failed: (lhs == rhs) lhs <- %s, rhs <- %s",
                lhs, rhs);

            @SuppressWarnings("UnnecessaryLocalVariable")
            AssertionError up = new AssertionError(message);
            throw up;
        }
    }


    /** If this project is in debug mode, immediately fail with the given
     * message, which is formatted using
     * {@link String#format(String, Object...)}.
     * @param format Message to be given for the failure, formats.
     * @param args Arguments to be used for formatting the message.
     */
    public static void fail(String format, Object... args) {
        String message = String.format(format, args);
        throw new AssertionError(message);
    }
}
