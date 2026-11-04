package com.starkgroup.vms.dto;

import com.starkgroup.vms.domain.Driver;
import com.starkgroup.vms.domain.DriverStatus;

public class DriverSummaryDto {

    private Long id;
    private String name;
    private String licenseNumber;
    private String phone;
    private DriverStatus status;

    public DriverSummaryDto() {
    }

    public DriverSummaryDto(Long id, String name, String licenseNumber, String phone, DriverStatus status) {
        this.id = id;
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.phone = phone;
        this.status = status;
    }

    public static DriverSummaryDto fromEntity(Driver driver) {
        if (driver == null) return null;
        return new DriverSummaryDto(
                driver.getId(),
                driver.getName(),
                driver.getLicenseNumber(),
                driver.getPhone(),
                driver.getStatus()
        );
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

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }
}

