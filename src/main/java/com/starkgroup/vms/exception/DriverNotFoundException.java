package com.starkgroup.vms.exception;

public class DriverNotFoundException extends RuntimeException {

    public DriverNotFoundException(Long id) {
        super("Driver not found with id: " + id);
    }

    public DriverNotFoundException(String message) {
        super(message);
    }
}

