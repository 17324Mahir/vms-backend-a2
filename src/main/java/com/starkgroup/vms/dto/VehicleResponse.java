package com.starkgroup.vms.dto;

import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.domain.VehicleStatus;

import java.math.BigDecimal;

public record VehicleResponse(
    Long id,
    String vehicleNumber,
    String company,
    String type,
    String brand,
    String model,
    VehicleStatus status,
    DriverSummaryDto assignedDriver,
    Long documentCount,
    Long maintenanceCount,
    BigDecimal totalMaintenanceCost
) {
    public VehicleResponse(
        Long id,
        String vehicleNumber,
        String company,
        String type,
        String brand,
        String model,
        VehicleStatus status
    ) {
        this(id, vehicleNumber, company, type, brand, model, status, null, 0L, 0L, BigDecimal.ZERO);
    }

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
            vehicle.getStatus(),
            DriverSummaryDto.fromEntity(vehicle.getAssignedDriver()),
            0L,
            0L,
            BigDecimal.ZERO
        );
    }

    public static VehicleResponse from(Vehicle vehicle, Long documentCount, Long maintenanceCount, BigDecimal totalMaintenanceCost) {
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
            vehicle.getStatus(),
            DriverSummaryDto.fromEntity(vehicle.getAssignedDriver()),
            documentCount != null ? documentCount : 0L,
            maintenanceCount != null ? maintenanceCount : 0L,
            totalMaintenanceCost != null ? totalMaintenanceCost : BigDecimal.ZERO
        );
    }
}
