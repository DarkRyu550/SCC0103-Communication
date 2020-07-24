package net.xn__n6x.communication.watchdog;

public class WatchdogException extends Exception {
    public WatchdogException() {
    }

    public WatchdogException(String message) {
        super(message);
    }

    public WatchdogException(String message, Throwable cause) {
        super(message, cause);
    }

    public WatchdogException(Throwable cause) {
        super(cause);
    }
}
