package com.starkgroup.vms.exception;

public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(String message) {
        super(message);
    }

    public VehicleNotFoundException(Long id) {
        super("Vehicle not found with ID: " + id);
    }
}
