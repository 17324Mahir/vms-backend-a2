package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.MaintenanceRecord;
import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.dto.CreateMaintenanceRequest;
import com.starkgroup.vms.dto.MaintenanceResponse;
import com.starkgroup.vms.dto.UpdateMaintenanceRequest;
import com.starkgroup.vms.exception.ResourceNotFoundException;
import com.starkgroup.vms.exception.VehicleNotFoundException;
import com.starkgroup.vms.repository.MaintenanceRecordRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.util.SecurityUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@Validated
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRecordRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final Validator validator;

    public MaintenanceServiceImpl(MaintenanceRecordRepository maintenanceRepository, VehicleRepository vehicleRepository) {
        this(maintenanceRepository, vehicleRepository, Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Autowired
    public MaintenanceServiceImpl(MaintenanceRecordRepository maintenanceRepository,
                                  VehicleRepository vehicleRepository,
                                  Validator validator) {
        this.maintenanceRepository = maintenanceRepository;
        this.vehicleRepository = vehicleRepository;
        this.validator = validator != null ? validator : Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    @Transactional
    public MaintenanceResponse recordMaintenance(Long vehicleId, CreateMaintenanceRequest request) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        String userEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle vehicle = vehicleRepository.findByIdAndUserEmailIgnoreCase(vehicleId, userEmail)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        validate(request);

        MaintenanceRecord record = new MaintenanceRecord(
                vehicle,
                request.getMaintenanceDate(),
                request.getMaintenanceType(),
                request.getCost(),
                request.getWorkshopVendor().trim(),
                request.getOdometerReading(),
                request.getNotes() != null ? request.getNotes().trim() : null
        );

        MaintenanceRecord saved = maintenanceRepository.save(record);

        // Optional status transition (e.g. set to MAINTENANCE or back to ACTIVE)
        if (request.getUpdateVehicleStatus() != null) {
            vehicle.setStatus(request.getUpdateVehicleStatus());
            vehicleRepository.save(vehicle);
        }

        return MaintenanceResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getMaintenanceHistoryForVehicle(Long vehicleId) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        String userEmail = SecurityUtils.getCurrentUserEmail();
        Vehicle vehicle = vehicleRepository.findByIdAndUserEmailIgnoreCase(vehicleId, userEmail)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        return maintenanceRepository.findByVehicleIdOrderByMaintenanceDateDesc(vehicleId).stream()
                .map(MaintenanceResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getAllMaintenanceForCurrentUser() {
        String userEmail = SecurityUtils.getCurrentUserEmail();
        return maintenanceRepository.findAllByUserEmailOrderByMaintenanceDateDesc(userEmail).stream()
                .map(MaintenanceResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceResponse getMaintenanceById(Long maintenanceId) {
        if (maintenanceId == null) {
            throw new ResourceNotFoundException("Maintenance ID must not be null");
        }
        String userEmail = SecurityUtils.getCurrentUserEmail();
        MaintenanceRecord record = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId));
        if (record.getVehicle() != null && record.getVehicle().getUserEmail() != null &&
                !record.getVehicle().getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId);
        }
        return MaintenanceResponse.from(record);
    }

    @Override
    @Transactional
    public MaintenanceResponse updateMaintenance(Long vehicleId, Long maintenanceId, UpdateMaintenanceRequest request) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        if (maintenanceId == null) {
            throw new ResourceNotFoundException("Maintenance ID must not be null");
        }
        String userEmail = SecurityUtils.getCurrentUserEmail();
        vehicleRepository.findByIdAndUserEmailIgnoreCase(vehicleId, userEmail)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        MaintenanceRecord record = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId));

        if (!record.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Maintenance record does not belong to the specified vehicle");
        }
        if (record.getVehicle().getUserEmail() != null &&
                !record.getVehicle().getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId);
        }

        validate(request);

        record.setMaintenanceDate(request.getMaintenanceDate());
        record.setMaintenanceType(request.getMaintenanceType());
        record.setCost(request.getCost());
        record.setWorkshopVendor(request.getWorkshopVendor().trim());
        record.setOdometerReading(request.getOdometerReading());
        record.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        MaintenanceRecord updated = maintenanceRepository.save(record);

        if (request.getUpdateVehicleStatus() != null) {
            Vehicle vehicle = record.getVehicle();
            vehicle.setStatus(request.getUpdateVehicleStatus());
            vehicleRepository.save(vehicle);
        }

        return MaintenanceResponse.from(updated);
    }

    @Override
    @Transactional
    public void deleteMaintenance(Long vehicleId, Long maintenanceId) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        if (maintenanceId == null) {
            throw new ResourceNotFoundException("Maintenance ID must not be null");
        }
        String userEmail = SecurityUtils.getCurrentUserEmail();
        vehicleRepository.findByIdAndUserEmailIgnoreCase(vehicleId, userEmail)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        MaintenanceRecord record = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId));

        if (!record.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Maintenance record does not belong to the specified vehicle");
        }
        if (record.getVehicle().getUserEmail() != null &&
                !record.getVehicle().getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId);
        }

        maintenanceRepository.delete(record);
    }

    @Override
    @Transactional
    public MaintenanceResponse updateMaintenanceById(Long maintenanceId, UpdateMaintenanceRequest request) {
        if (maintenanceId == null) {
            throw new ResourceNotFoundException("Maintenance ID must not be null");
        }
        String userEmail = SecurityUtils.getCurrentUserEmail();
        MaintenanceRecord record = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId));

        if (record.getVehicle() != null && record.getVehicle().getUserEmail() != null &&
                !record.getVehicle().getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId);
        }

        validate(request);

        record.setMaintenanceDate(request.getMaintenanceDate());
        record.setMaintenanceType(request.getMaintenanceType());
        record.setCost(request.getCost());
        record.setWorkshopVendor(request.getWorkshopVendor().trim());
        record.setOdometerReading(request.getOdometerReading());
        record.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        MaintenanceRecord updated = maintenanceRepository.save(record);

        if (request.getUpdateVehicleStatus() != null) {
            Vehicle vehicle = record.getVehicle();
            vehicle.setStatus(request.getUpdateVehicleStatus());
            vehicleRepository.save(vehicle);
        }

        return MaintenanceResponse.from(updated);
    }

    @Override
    @Transactional
    public void deleteMaintenanceById(Long maintenanceId) {
        if (maintenanceId == null) {
            throw new ResourceNotFoundException("Maintenance ID must not be null");
        }
        String userEmail = SecurityUtils.getCurrentUserEmail();
        MaintenanceRecord record = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId));

        if (record.getVehicle() != null && record.getVehicle().getUserEmail() != null &&
                !record.getVehicle().getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId);
        }

        maintenanceRepository.delete(record);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalMaintenanceCost() {
        BigDecimal total = maintenanceRepository.calculateTotalMaintenanceCost();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalMaintenanceCostForVehicle(Long vehicleId) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        BigDecimal total = maintenanceRepository.calculateTotalMaintenanceCostForVehicle(vehicleId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalMaintenanceRecords() {
        return maintenanceRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getRecentMaintenance(int limit) {
        List<MaintenanceRecord> records = maintenanceRepository.findTop10ByOrderByMaintenanceDateDesc();
        if (records.size() > limit) {
            records = records.subList(0, limit);
        }
        return records.stream().map(MaintenanceResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalMaintenanceCostForUser(String userEmail) {
        BigDecimal total = maintenanceRepository.calculateTotalMaintenanceCostByUser(userEmail);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalMaintenanceRecordsForUser(String userEmail) {
        return maintenanceRepository.countByUserEmail(userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getRecentMaintenanceForUser(String userEmail, int limit) {
        List<MaintenanceRecord> records = maintenanceRepository.findRecentByUser(userEmail, PageRequest.of(0, limit));
        return records.stream().map(MaintenanceResponse::from).toList();
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

