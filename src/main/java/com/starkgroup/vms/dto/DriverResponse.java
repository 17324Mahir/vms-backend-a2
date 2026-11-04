package com.starkgroup.vms.dto;

import com.starkgroup.vms.domain.Driver;
import com.starkgroup.vms.domain.DriverStatus;
import com.starkgroup.vms.domain.Vehicle;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class DriverResponse {

    private Long id;
    private String name;
    private String licenseNumber;
    private String phone;
    private String email;
    private DriverStatus status;
    private LocalDate licenseExpiryDate;
    private Long assignedVehicleId;
    private String assignedVehicleNumber;
    private String expiryStatus; // EXPIRED, EXPIRING_SOON, VALID
    private Long daysUntilExpiry;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public DriverResponse() {
    }

    public static DriverResponse fromEntity(Driver driver) {
        return fromEntity(driver, null);
    }

    public static DriverResponse fromEntity(Driver driver, Vehicle assignedVehicle) {
        if (driver == null) return null;
        DriverResponse resp = new DriverResponse();
        resp.setId(driver.getId());
        resp.setName(driver.getName());
        resp.setLicenseNumber(driver.getLicenseNumber());
        resp.setPhone(driver.getPhone());
        resp.setEmail(driver.getEmail());
        resp.setStatus(driver.getStatus());
        resp.setLicenseExpiryDate(driver.getLicenseExpiryDate());
        resp.setCreatedAt(driver.getCreatedAt());
        resp.setUpdatedAt(driver.getUpdatedAt());

        if (assignedVehicle != null) {
            resp.setAssignedVehicleId(assignedVehicle.getId());
            resp.setAssignedVehicleNumber(assignedVehicle.getVehicleNumber());
        }

        if (driver.getLicenseExpiryDate() != null) {
            LocalDate today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(today, driver.getLicenseExpiryDate());
            resp.setDaysUntilExpiry(days);
            if (days < 0) {
                resp.setExpiryStatus("EXPIRED");
            } else if (days <= 30) {
                resp.setExpiryStatus("EXPIRING_SOON");
            } else {
                resp.setExpiryStatus("VALID");
            }
        } else {
            resp.setExpiryStatus("VALID");
            resp.setDaysUntilExpiry(null);
        }

        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }

    public LocalDate getLicenseExpiryDate() {
        return licenseExpiryDate;
    }

    public void setLicenseExpiryDate(LocalDate licenseExpiryDate) {
        this.licenseExpiryDate = licenseExpiryDate;
    }

    public Long getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public void setAssignedVehicleId(Long assignedVehicleId) {
        this.assignedVehicleId = assignedVehicleId;
    }

    public String getAssignedVehicleNumber() {
        return assignedVehicleNumber;
    }

    public void setAssignedVehicleNumber(String assignedVehicleNumber) {
        this.assignedVehicleNumber = assignedVehicleNumber;
    }

    public String getExpiryStatus() {
        return expiryStatus;
    }

    public void setExpiryStatus(String expiryStatus) {
        this.expiryStatus = expiryStatus;
    }

    public Long getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public void setDaysUntilExpiry(Long daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
