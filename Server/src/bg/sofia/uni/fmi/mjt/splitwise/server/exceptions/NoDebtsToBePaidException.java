package bg.sofia.uni.fmi.mjt.splitwise.server.exceptions;

public class NoDebtsToBePaidException extends Exception {
    public NoDebtsToBePaidException(String message) {
        super(message);
    }

    public NoDebtsToBePaidException(String message, Throwable cause) {
        super(message, cause);
    }
}
