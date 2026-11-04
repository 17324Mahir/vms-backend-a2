package com.starkgroup.vms.exception;

/**
 * Thrown when login authentication fails due to incorrect email or password.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
