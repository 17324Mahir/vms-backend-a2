package com.starkgroup.vms.dto;

import com.starkgroup.vms.domain.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class CreateDriverRequest {

    @NotBlank(message = "Driver name is required")
    @Size(max = 120, message = "Driver name must not exceed 120 characters")
    private String name;

    @NotBlank(message = "License number is required")
    @Size(max = 60, message = "License number must not exceed 60 characters")
    private String licenseNumber;

    @NotBlank(message = "Phone number is required")
    @Size(max = 40, message = "Phone number must not exceed 40 characters")
    private String phone;

    @Size(max = 120, message = "Email must not exceed 120 characters")
    private String email;

    private DriverStatus status = DriverStatus.AVAILABLE;

    private LocalDate licenseExpiryDate;

    public CreateDriverRequest() {
    }

    public CreateDriverRequest(String name, String licenseNumber, String phone, String email, DriverStatus status, LocalDate licenseExpiryDate) {
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.licenseExpiryDate = licenseExpiryDate;
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
}

