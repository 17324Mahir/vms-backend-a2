package com.starkgroup.vms.repository;

import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.domain.VehicleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VehiclePersistenceTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should persist vehicle, generate ID, and store status as string in PostgreSQL")
    void shouldPersistVehicleAndStoreStatusAsString() {
        Vehicle vehicle = new Vehicle(
                "TEST-SG-001",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model 3",
                VehicleStatus.ACTIVE
        );

        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);

        assertNotNull(saved.getId(), "Database should generate primary key ID");
        assertTrue(saved.getId() > 0, "Generated ID should be positive");

        // Verify direct SQL query confirms column types and string representation of enum
        String persistedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM vehicles WHERE id = ?",
                String.class,
                saved.getId()
        );
        assertEquals("ACTIVE", persistedStatus, "VehicleStatus enum must be persisted as String, not ordinal");
    }

    @Test
    @DisplayName("Should enforce unique constraint on vehicle_number at the database level")
    void shouldEnforceDatabaseLevelUniqueConstraintOnVehicleNumber() {
        Vehicle vehicle1 = new Vehicle(
                "TEST-UNIQUE-001",
                "Stark Industries",
                "SUV",
                "Tesla",
                "Model X",
                VehicleStatus.ACTIVE
        );
        vehicleRepository.saveAndFlush(vehicle1);

        Vehicle vehicle2 = new Vehicle(
                "TEST-UNIQUE-001",
                "Stark Logistics",
                "Truck",
                "Volvo",
                "FH16",
                VehicleStatus.INACTIVE
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            vehicleRepository.saveAndFlush(vehicle2);
        }, "Database unique constraint uk_vehicles_vehicle_number must reject duplicate vehicle numbers");
    }

    @Test
    @DisplayName("Should enforce NOT NULL constraints at database level")
    void shouldEnforceNotNullConstraints() {
        Vehicle invalidVehicle = new Vehicle(
                null,
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            vehicleRepository.saveAndFlush(invalidVehicle);
        });
    }

    @Test
    @DisplayName("Should enforce CHECK constraint on status column at database level")
    void shouldEnforceStatusCheckConstraint() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO vehicles (vehicle_number, company, type, brand, model, status) VALUES (?, ?, ?, ?, ?, ?)",
                    "TEST-INVALID-STATUS", "Stark Corp", "Van", "Ford", "Transit", "FLYING"
            );
        }, "Database check constraint chk_vehicles_status must reject unsupported status values");
    }
}
