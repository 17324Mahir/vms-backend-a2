package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.dto.VehicleResponse;
import com.starkgroup.vms.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.exception.VehicleNotFoundException;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.util.VehicleNumberNormalizer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Validated
public class VehicleServiceImpl implements VehicleService {

    private static final String DUPLICATE_VEHICLE_NUMBER_MESSAGE =
            "A vehicle with this Vehicle Number already exists.";

    private final VehicleRepository vehicleRepository;
    private final Validator validator;

    public VehicleServiceImpl(VehicleRepository vehicleRepository) {
        this(vehicleRepository, Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Autowired
    public VehicleServiceImpl(VehicleRepository vehicleRepository, Validator validator) {
        this.vehicleRepository = vehicleRepository;
        this.validator = validator != null ? validator : Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        validate(request);

        String normalizedNumber = VehicleNumberNormalizer.normalize(request.vehicleNumber());

        // 1. Business-layer duplicate check
        if (vehicleRepository.existsByVehicleNumber(normalizedNumber)) {
            throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber(normalizedNumber);
        vehicle.setCompany(request.company().trim());
        vehicle.setType(request.type().trim());
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setStatus(request.status());

        // 2. Database-level persistence and unique constraint handling
        try {
            Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
            return VehicleResponse.from(saved);
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
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
        return VehicleResponse.from(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {
        if (id == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }

        Vehicle existing = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));

        validate(request);

        String normalizedNumber = VehicleNumberNormalizer.normalize(request.vehicleNumber());

        // 1. Business-layer duplicate check (excluding the current vehicle)
        if (vehicleRepository.existsByVehicleNumberAndIdNot(normalizedNumber, id)) {
            throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE);
        }

        // Update existing row while preserving original ID
        existing.setVehicleNumber(normalizedNumber);
        existing.setCompany(request.company().trim());
        existing.setType(request.type().trim());
        existing.setBrand(request.brand().trim());
        existing.setModel(request.model().trim());
        existing.setStatus(request.status());

        // 2. Database-level persistence and unique constraint handling
        try {
            Vehicle updated = vehicleRepository.saveAndFlush(existing);
            return VehicleResponse.from(updated);
        } catch (DataIntegrityViolationException ex) {
            if (isVehicleNumberUniqueConstraintViolation(ex)) {
                throw new DuplicateVehicleNumberException(DUPLICATE_VEHICLE_NUMBER_MESSAGE, ex);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        if (id == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));

        vehicleRepository.delete(vehicle);
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

    private boolean isVehicleNumberUniqueConstraintViolation(DataIntegrityViolationException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if (constraintName != null && matchesConstraint(constraintName)) {
                    return true;
                }
            }
            String msg = current.getMessage();
            if (msg != null && matchesConstraint(msg)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean matchesConstraint(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("uk_vehicles_vehicle_number")
                || (lower.contains("vehicle_number") && lower.contains("unique"));
    }
}
