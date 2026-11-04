package com.starkgroup.vms.exception;

public class DuplicateLicenseNumberException extends RuntimeException {

    public DuplicateLicenseNumberException(String message) {
        super(message);
    }

    public DuplicateLicenseNumberException(String message, Throwable cause) {
        super(message, cause);
    }
}

