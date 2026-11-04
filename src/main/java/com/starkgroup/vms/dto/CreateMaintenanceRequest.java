package com.starkgroup.vms.dto;

import com.starkgroup.vms.domain.MaintenanceType;
import com.starkgroup.vms.domain.VehicleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateMaintenanceRequest {

    @NotNull(message = "Maintenance date is required")
    private LocalDate maintenanceDate;

    @NotNull(message = "Maintenance type is required")
    private MaintenanceType maintenanceType;

    @NotNull(message = "Cost is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost must be greater than or equal to 0")
    private BigDecimal cost;

    @NotBlank(message = "Workshop/vendor is required")
    @Size(max = 160, message = "Workshop/vendor must not exceed 160 characters")
    private String workshopVendor;

    private Integer odometerReading;

    private String notes;

    private VehicleStatus updateVehicleStatus;

    public CreateMaintenanceRequest() {
    }

    public CreateMaintenanceRequest(LocalDate maintenanceDate, MaintenanceType maintenanceType, BigDecimal cost,
                                    String workshopVendor, Integer odometerReading, String notes, VehicleStatus updateVehicleStatus) {
        this.maintenanceDate = maintenanceDate;
        this.maintenanceType = maintenanceType;
        this.cost = cost;
        this.workshopVendor = workshopVendor;
        this.odometerReading = odometerReading;
        this.notes = notes;
        this.updateVehicleStatus = updateVehicleStatus;
    }

    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(LocalDate maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    public MaintenanceType getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(MaintenanceType maintenanceType) {
        this.maintenanceType = maintenanceType;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getWorkshopVendor() {
        return workshopVendor;
    }

    public void setWorkshopVendor(String workshopVendor) {
        this.workshopVendor = workshopVendor;
    }

    public Integer getOdometerReading() {
        return odometerReading;
    }

    public void setOdometerReading(Integer odometerReading) {
        this.odometerReading = odometerReading;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public VehicleStatus getUpdateVehicleStatus() {
        return updateVehicleStatus;
    }

    public void setUpdateVehicleStatus(VehicleStatus updateVehicleStatus) {
        this.updateVehicleStatus = updateVehicleStatus;
    }
}

