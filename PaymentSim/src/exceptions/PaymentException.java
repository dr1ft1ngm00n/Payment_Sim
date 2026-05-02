package exceptions;

/** Thrown when a payment operation fails for any reason. */
public class PaymentException extends Exception {
    public PaymentException(String message) {
        super(message);
    }
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
