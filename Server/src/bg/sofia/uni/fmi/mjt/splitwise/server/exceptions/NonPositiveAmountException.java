package bg.sofia.uni.fmi.mjt.splitwise.server.exceptions;

public class NonPositiveAmountException extends Exception {
    public NonPositiveAmountException(String message) {
        super(message);
    }

    public NonPositiveAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}
