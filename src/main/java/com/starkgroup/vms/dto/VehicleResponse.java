package com.starkgroup.vms.dto;

import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.domain.VehicleStatus;

public record VehicleResponse(
    Long id,
    String vehicleNumber,
    String company,
    String type,
    String brand,
    String model,
    VehicleStatus status
) {
    public static VehicleResponse from(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehicleResponse(
            vehicle.getId(),
            vehicle.getVehicleNumber(),
            vehicle.getCompany(),
            vehicle.getType(),
            vehicle.getBrand(),
            vehicle.getModel(),
            vehicle.getStatus()
        );
    }
}
