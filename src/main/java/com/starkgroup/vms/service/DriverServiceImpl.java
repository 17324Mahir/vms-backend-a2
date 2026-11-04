package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.Driver;
import com.starkgroup.vms.domain.DriverStatus;
import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.dto.CreateDriverRequest;
import com.starkgroup.vms.dto.DriverResponse;
import com.starkgroup.vms.dto.DriverSummaryDto;
import com.starkgroup.vms.dto.PageResponse;
import com.starkgroup.vms.dto.UpdateDriverRequest;
import com.starkgroup.vms.exception.DriverNotFoundException;
import com.starkgroup.vms.exception.DuplicateLicenseNumberException;
import com.starkgroup.vms.repository.DriverRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final Validator validator;

    public DriverServiceImpl(DriverRepository driverRepository, VehicleRepository vehicleRepository) {
        this(driverRepository, vehicleRepository, Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Autowired
    public DriverServiceImpl(DriverRepository driverRepository, VehicleRepository vehicleRepository, Validator validator) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.validator = validator != null ? validator : Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    @Transactional
    public DriverResponse createDriver(CreateDriverRequest request) {
        validate(request);

        String normalizedLicense = request.getLicenseNumber().trim();
        if (driverRepository.existsByLicenseNumberIgnoreCase(normalizedLicense)) {
            throw new DuplicateLicenseNumberException("A driver with license number '" + normalizedLicense + "' already exists.");
        }

        Driver driver = new Driver();
        driver.setName(request.getName().trim());
        driver.setLicenseNumber(normalizedLicense);
        driver.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        driver.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null);
        driver.setStatus(request.getStatus() != null ? request.getStatus() : DriverStatus.AVAILABLE);
        driver.setLicenseExpiryDate(request.getLicenseExpiryDate());

        Driver saved = driverRepository.save(driver);
        return DriverResponse.fromEntity(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse getDriverById(Long id) {
        if (id == null) {
            throw new DriverNotFoundException("Driver ID must not be null");
        }
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id));

        List<Vehicle> assignedVehicles = vehicleRepository.findByAssignedDriverId(id);
        Vehicle assignedVehicle = assignedVehicles.isEmpty() ? null : assignedVehicles.get(0);

        return DriverResponse.fromEntity(driver, assignedVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers() {
        List<Driver> drivers = driverRepository.findAll();
        Map<Long, Vehicle> driverToVehicleMap = vehicleRepository.findAll().stream()
                .filter(v -> v.getAssignedDriver() != null)
                .collect(Collectors.toMap(v -> v.getAssignedDriver().getId(), v -> v, (v1, v2) -> v1));

        return drivers.stream()
                .map(d -> DriverResponse.fromEntity(d, driverToVehicleMap.get(d.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverSummaryDto> getAvailableDrivers() {
        return driverRepository.findAll().stream()
                .map(DriverSummaryDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DriverResponse> searchDrivers(String search, DriverStatus status, Pageable pageable) {
        Page<Driver> page = driverRepository.searchDrivers(search, status, pageable);

        Map<Long, Vehicle> driverToVehicleMap = vehicleRepository.findAll().stream()
                .filter(v -> v.getAssignedDriver() != null)
                .collect(Collectors.toMap(v -> v.getAssignedDriver().getId(), v -> v, (v1, v2) -> v1));

        List<DriverResponse> content = page.getContent().stream()
                .map(d -> DriverResponse.fromEntity(d, driverToVehicleMap.get(d.getId())))
                .toList();

        return PageResponse.of(content, page);
    }

    @Override
    @Transactional
    public DriverResponse updateDriver(Long id, UpdateDriverRequest request) {
        if (id == null) {
            throw new DriverNotFoundException("Driver ID must not be null");
        }

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id));

        validate(request);

        String normalizedLicense = request.getLicenseNumber().trim();
        if (driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot(normalizedLicense, id)) {
            throw new DuplicateLicenseNumberException("A driver with license number '" + normalizedLicense + "' already exists.");
        }

        driver.setName(request.getName().trim());
        driver.setLicenseNumber(normalizedLicense);
        driver.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        driver.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null);
        driver.setStatus(request.getStatus() != null ? request.getStatus() : driver.getStatus());
        driver.setLicenseExpiryDate(request.getLicenseExpiryDate());

        Driver updated = driverRepository.save(driver);

        List<Vehicle> assignedVehicles = vehicleRepository.findByAssignedDriverId(id);
        Vehicle assignedVehicle = assignedVehicles.isEmpty() ? null : assignedVehicles.get(0);

        return DriverResponse.fromEntity(updated, assignedVehicle);
    }

    @Override
    @Transactional
    public void deleteDriver(Long id) {
        if (id == null) {
            throw new DriverNotFoundException("Driver ID must not be null");
        }

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id));

        // Unassign any vehicles before deleting driver
        List<Vehicle> assignedVehicles = vehicleRepository.findByAssignedDriverId(id);
        for (Vehicle v : assignedVehicles) {
            v.setAssignedDriver(null);
            vehicleRepository.save(v);
        }

        driverRepository.delete(driver);
    }

    private <T> void validate(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
