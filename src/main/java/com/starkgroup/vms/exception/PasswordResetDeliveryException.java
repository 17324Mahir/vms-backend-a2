package com.starkgroup.vms.exception;

public class PasswordResetDeliveryException extends RuntimeException {

    public PasswordResetDeliveryException() {
        super("Unable to send a password reset code right now. Please try again later.");
    }
}
