package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.Driver;
import com.starkgroup.vms.domain.DriverStatus;
import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.PageResponse;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.dto.VehicleResponse;
import com.starkgroup.vms.exception.DriverNotFoundException;
import com.starkgroup.vms.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.exception.VehicleNotFoundException;
import com.starkgroup.vms.repository.DriverRepository;
import com.starkgroup.vms.repository.MaintenanceRecordRepository;
import com.starkgroup.vms.repository.VehicleDocumentRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.util.SecurityUtils;
import com.starkgroup.vms.util.VehicleNumberNormalizer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@Validated
public class VehicleServiceImpl implements VehicleService {

    private static final String DUPLICATE_VEHICLE_NUMBER_MESSAGE =
            "A vehicle with this Vehicle Number already exists.";

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final VehicleDocumentRepository documentRepository;
    private final MaintenanceRecordRepository maintenanceRepository;
    private final Validator validator;

    public VehicleServiceImpl(VehicleRepository vehicleRepository) {
        this(vehicleRepository, null, null, null, Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Autowired
    public VehicleServiceImpl(VehicleRepository vehicleRepository,
                              DriverRepository driverRepository,
                              VehicleDocumentRepository documentRepository,
                              MaintenanceRecordRepository maintenanceRepository,
                              Validator validator) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.documentRepository = documentRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.validator = validator != null ? validator : Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        validate(request);

        String userEmail = SecurityUtils.getCurrentUserEmail();
        String normalizedNumber = VehicleNumberNormalizer.normalize(request.vehicleNumber());

        if (checkVehicleNumberExists(normalizedNumber, userEmail)) {
            throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUserEmail(userEmail);
        vehicle.setVehicleNumber(normalizedNumber);
        vehicle.setCompany(request.company().trim());
        vehicle.setType(request.type().trim());
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setStatus(request.status());

        if (request.assignedDriverId() != null && driverRepository != null) {
            Driver driver = findDriverScoped(request.assignedDriverId(), userEmail)
                    .orElseThrow(() -> new DriverNotFoundException(request.assignedDriverId()));
            vehicle.setAssignedDriver(driver);
            driver.setStatus(DriverStatus.ASSIGNED);
            driverRepository.save(driver);
        }

        try {
            Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
            return toDetailedResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isVehicleNumberUniqueConstraintViolation(ex)) {
                throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE, ex);
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        if (id == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        String userEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle vehicle = findVehicleScoped(id, userEmail)
                .orElseThrow(() -> new VehicleNotFoundException(id));
        return toDetailedResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        String userEmail = SecurityUtils.getCurrentUserEmail();
        List<Vehicle> list = vehicleRepository.findAllByUserEmailIgnoreCase(userEmail);
        return list.stream()
                .map(this::toDetailedResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getVehicleCompanies() {
        return vehicleRepository.findCompaniesByUser(SecurityUtils.getCurrentUserEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> searchVehicles(String search, VehicleStatus status, String company, Pageable pageable) {
        String userEmail = SecurityUtils.getCurrentUserEmail();
        Page<Vehicle> page = vehicleRepository.searchVehiclesByUser(userEmail, search, status, company, pageable);
        List<VehicleResponse> content = page.getContent().stream()
                .map(this::toDetailedResponse)
                .toList();

        return PageResponse.of(content, page);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {
        if (id == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }

        String userEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle existing = findVehicleScoped(id, userEmail)
                .orElseThrow(() -> new VehicleNotFoundException(id));

        validate(request);

        String normalizedNumber = VehicleNumberNormalizer.normalize(request.vehicleNumber());

        if (checkVehicleNumberExistsAndIdNot(normalizedNumber, userEmail, id)) {
            throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE);
        }

        existing.setVehicleNumber(normalizedNumber);
        existing.setCompany(request.company().trim());
        existing.setType(request.type().trim());
        existing.setBrand(request.brand().trim());
        existing.setModel(request.model().trim());
        existing.setStatus(request.status());

        // Manage driver assignment change within user scope
        if (driverRepository != null) {
            Long newDriverId = request.assignedDriverId();
            Driver previousDriver = existing.getAssignedDriver();

            if (newDriverId == null) {
                if (previousDriver != null) {
                    existing.setAssignedDriver(null);
                    updateDriverStatusIfNoOtherVehicles(previousDriver.getId(), userEmail);
                }
            } else if (previousDriver == null || !previousDriver.getId().equals(newDriverId)) {
                Driver newDriver = findDriverScoped(newDriverId, userEmail)
                        .orElseThrow(() -> new DriverNotFoundException(newDriverId));
                existing.setAssignedDriver(newDriver);
                newDriver.setStatus(DriverStatus.ASSIGNED);
                driverRepository.save(newDriver);

                if (previousDriver != null) {
                    updateDriverStatusIfNoOtherVehicles(previousDriver.getId(), userEmail);
                }
            }
        }

        try {
            Vehicle updated = vehicleRepository.saveAndFlush(existing);
            return toDetailedResponse(updated);
        } catch (DataIntegrityViolationException ex) {
            if (isVehicleNumberUniqueConstraintViolation(ex)) {
                throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE, ex);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicleStatus(Long id, VehicleStatus status) {
        if (id == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        String userEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle vehicle = findVehicleScoped(id, userEmail)
                .orElseThrow(() -> new VehicleNotFoundException(id));
        vehicle.setStatus(status);
        Vehicle saved = vehicleRepository.save(vehicle);
        return toDetailedResponse(saved);
    }

    @Override
    @Transactional
    public VehicleResponse assignDriver(Long vehicleId, Long driverId) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        if (driverId == null) {
            throw new DriverNotFoundException("Driver ID must not be null");
        }

        String userEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle vehicle = findVehicleScoped(vehicleId, userEmail)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        if (driverRepository == null) {
            return toDetailedResponse(vehicle);
        }

        Driver driver = findDriverScoped(driverId, userEmail)
                .orElseThrow(() -> new DriverNotFoundException(driverId));

        Driver previousDriver = vehicle.getAssignedDriver();
        vehicle.setAssignedDriver(driver);
        driver.setStatus(DriverStatus.ASSIGNED);
        driverRepository.save(driver);

        if (previousDriver != null && !previousDriver.getId().equals(driverId)) {
            updateDriverStatusIfNoOtherVehicles(previousDriver.getId(), userEmail);
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        return toDetailedResponse(saved);
    }

    @Override
    @Transactional
    public VehicleResponse unassignDriver(Long vehicleId) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        String userEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle vehicle = findVehicleScoped(vehicleId, userEmail)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        Driver previousDriver = vehicle.getAssignedDriver();
        if (previousDriver != null) {
            vehicle.setAssignedDriver(null);
            updateDriverStatusIfNoOtherVehicles(previousDriver.getId(), userEmail);
            Vehicle saved = vehicleRepository.save(vehicle);
            return toDetailedResponse(saved);
        }

        return toDetailedResponse(vehicle);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        if (id == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }

        String userEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle vehicle = findVehicleScoped(id, userEmail)
                .orElseThrow(() -> new VehicleNotFoundException(id));

        Driver previousDriver = vehicle.getAssignedDriver();
        vehicleRepository.delete(vehicle);

        if (previousDriver != null) {
            updateDriverStatusIfNoOtherVehicles(previousDriver.getId(), userEmail);
        }
    }

    private Optional<Vehicle> findVehicleScoped(Long id, String userEmail) {
        return vehicleRepository.findByIdAndUserEmailIgnoreCase(id, userEmail);
    }

    private Optional<Driver> findDriverScoped(Long id, String userEmail) {
        if (driverRepository == null) return Optional.empty();
        return driverRepository.findByIdAndUserEmailIgnoreCase(id, userEmail);
    }

    private boolean checkVehicleNumberExists(String vehicleNumber, String userEmail) {
        return vehicleRepository.existsByVehicleNumberIgnoreCaseAndUserEmailIgnoreCase(vehicleNumber, userEmail);
    }

    private boolean checkVehicleNumberExistsAndIdNot(String vehicleNumber, String userEmail, Long id) {
        return vehicleRepository.existsByVehicleNumberIgnoreCaseAndUserEmailIgnoreCaseAndIdNot(vehicleNumber, userEmail, id);
    }

    private void updateDriverStatusIfNoOtherVehicles(Long driverId, String userEmail) {
        if (driverRepository == null || driverId == null) return;
        List<Vehicle> otherVehicles = vehicleRepository.findByUserEmailIgnoreCaseAndAssignedDriverId(userEmail, driverId);
        if (otherVehicles.isEmpty()) {
            findDriverScoped(driverId, userEmail).ifPresent(driver -> {
                driver.setStatus(DriverStatus.AVAILABLE);
                driverRepository.save(driver);
            });
        }
    }

    private VehicleResponse toDetailedResponse(Vehicle vehicle) {
        Long docCount = 0L;
        Long maintCount = 0L;
        BigDecimal totalCost = BigDecimal.ZERO;

        if (documentRepository != null && vehicle.getId() != null) {
            docCount = (long) documentRepository.findByVehicleId(vehicle.getId()).size();
        }
        if (maintenanceRepository != null && vehicle.getId() != null) {
            maintCount = (long) maintenanceRepository.findByVehicleIdOrderByMaintenanceDateDesc(vehicle.getId()).size();
            BigDecimal cost = maintenanceRepository.calculateTotalMaintenanceCostForVehicle(vehicle.getId());
            if (cost != null) {
                totalCost = cost;
            }
        }

        return VehicleResponse.from(vehicle, docCount, maintCount, totalCost);
    }

    private boolean isVehicleNumberUniqueConstraintViolation(DataIntegrityViolationException ex) {
        Throwable rootCause = ex.getRootCause();
        if (rootCause == null) {
            rootCause = ex;
        }
        String message = rootCause.getMessage();
        if (message == null) {
            return false;
        }
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        return lowerMessage.contains("uk_vehicles_vehicle_number")
                || lowerMessage.contains("uk_vehicles_user_vehicle_number")
                || lowerMessage.contains("vehicle_number")
                || lowerMessage.contains("duplicate key");
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
