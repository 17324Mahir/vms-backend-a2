package com.starkgroup.vms.exception;

/**
 * Exception thrown when attempting to register an email address that is already in use.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}

