package com.starkgroup.vms.dto;

import com.starkgroup.vms.domain.MaintenanceRecord;
import com.starkgroup.vms.domain.MaintenanceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record MaintenanceResponse(
    Long id,
    Long vehicleId,
    String vehicleNumber,
    LocalDate maintenanceDate,
    MaintenanceType maintenanceType,
    BigDecimal cost,
    String workshopVendor,
    Integer odometerReading,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static MaintenanceResponse from(MaintenanceRecord record) {
        if (record == null) {
            return null;
        }
        return new MaintenanceResponse(
            record.getId(),
            record.getVehicle() != null ? record.getVehicle().getId() : null,
            record.getVehicle() != null ? record.getVehicle().getVehicleNumber() : null,
            record.getMaintenanceDate(),
            record.getMaintenanceType(),
            record.getCost(),
            record.getWorkshopVendor(),
            record.getOdometerReading(),
            record.getNotes(),
            record.getCreatedAt(),
            record.getUpdatedAt()
        );
    }
}

