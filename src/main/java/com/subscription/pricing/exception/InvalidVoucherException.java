package com.subscription.pricing.exception;

/**
 * Exception thrown when a supplied voucher code is not recognized or has expired.
 */
public class InvalidVoucherException extends RuntimeException {

    public InvalidVoucherException(String message) {
        super(message);
    }

    public InvalidVoucherException(String message, Throwable cause) {
        super(message, cause);
    }
}
