package com.starkgroup.vms.dto;

import com.starkgroup.vms.domain.VehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVehicleRequest(
    @NotBlank(message = "Vehicle number must not be blank")
    @Size(max = 64, message = "Vehicle number must not exceed 64 characters")
    String vehicleNumber,

    @NotBlank(message = "Company must not be blank")
    @Size(max = 120, message = "Company must not exceed 120 characters")
    String company,

    @NotBlank(message = "Type must not be blank")
    @Size(max = 60, message = "Type must not exceed 60 characters")
    String type,

    @NotBlank(message = "Brand must not be blank")
    @Size(max = 80, message = "Brand must not exceed 80 characters")
    String brand,

    @NotBlank(message = "Model must not be blank")
    @Size(max = 120, message = "Model must not exceed 120 characters")
    String model,

    @NotNull(message = "Status is required")
    VehicleStatus status,

    Long assignedDriverId
) {
    public CreateVehicleRequest(
        String vehicleNumber,
        String company,
        String type,
        String brand,
        String model,
        VehicleStatus status
    ) {
        this(vehicleNumber, company, type, brand, model, status, null);
    }
}
