package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.dto.VehicleResponse;
import com.starkgroup.vms.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.exception.VehicleNotFoundException;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.util.SecurityUtils;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleServiceImpl(vehicleRepository);
    }

    // --- Create Vehicle Tests ---

    @Test
    @DisplayName("Duplicate Vehicle Number is rejected by business layer before persistence")
    void createVehicle_whenVehicleNumberAlreadyExists_throwsDuplicateVehicleNumberException() {
        // Arrange
        CreateVehicleRequest request = new CreateVehicleRequest(
                "STK-9001",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        when(vehicleRepository.existsByVehicleNumberIgnoreCaseAndUserEmailIgnoreCase("STK-9001", SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(true);

        // Act & Assert
        DuplicateVehicleNumberException ex = assertThrows(DuplicateVehicleNumberException.class,
                () -> vehicleService.createVehicle(request));

        assertEquals("A vehicle with this Vehicle Number already exists.", ex.getMessage());
        verify(vehicleRepository, never()).saveAndFlush(any());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("New unique Vehicle Number is accepted and persisted with generated ID")
    void createVehicle_whenVehicleNumberIsNew_createsVehicle() {
        // Arrange
        CreateVehicleRequest request = new CreateVehicleRequest(
                "STK-9002",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        when(vehicleRepository.existsByVehicleNumberIgnoreCaseAndUserEmailIgnoreCase("STK-9002", SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(false);
        when(vehicleRepository.saveAndFlush(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle toSave = invocation.getArgument(0);
            return new Vehicle(
                    101L,
                    toSave.getVehicleNumber(),
                    toSave.getCompany(),
                    toSave.getType(),
                    toSave.getBrand(),
                    toSave.getModel(),
                    toSave.getStatus()
            );
        });

        // Act
        VehicleResponse response = vehicleService.createVehicle(request);

        // Assert
        assertNotNull(response);
        assertEquals(101L, response.id());
        assertEquals("STK-9002", response.vehicleNumber());
        assertEquals("Stark Industries", response.company());
        assertEquals("Sedan", response.type());
        assertEquals("Tesla", response.brand());
        assertEquals("Model S", response.model());
        assertEquals(VehicleStatus.ACTIVE, response.status());

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository, times(1)).saveAndFlush(captor.capture());
        assertEquals("STK-9002", captor.getValue().getVehicleNumber());
        assertEquals("Stark Industries", captor.getValue().getCompany());
    }

    @Test
    @DisplayName("Vehicle Number with leading/trailing whitespace and mixed case is normalized and rejected as duplicate")
    void createVehicle_whenVehicleNumberHasWhitespaceAndMixedCase_normalizesAndRejectsDuplicate() {
        // Arrange
        CreateVehicleRequest request = new CreateVehicleRequest(
                "  stk-9001  ",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        when(vehicleRepository.existsByVehicleNumberIgnoreCaseAndUserEmailIgnoreCase("STK-9001", SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(true);

        // Act & Assert
        DuplicateVehicleNumberException ex = assertThrows(DuplicateVehicleNumberException.class,
                () -> vehicleService.createVehicle(request));

        assertEquals("A vehicle with this Vehicle Number already exists.", ex.getMessage());
        verify(vehicleRepository).existsByVehicleNumberIgnoreCaseAndUserEmailIgnoreCase("STK-9001", SecurityUtils.DEFAULT_USER_EMAIL);
        verify(vehicleRepository, never()).saveAndFlush(any());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid blank fields are rejected by validation")
    void createVehicle_withBlankFields_throwsConstraintViolationException() {
        CreateVehicleRequest blankNumber = new CreateVehicleRequest(
                "   ",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        assertThrows(ConstraintViolationException.class, () -> vehicleService.createVehicle(blankNumber));

        CreateVehicleRequest nullStatus = new CreateVehicleRequest(
                "SG-1002",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                null
        );

        assertThrows(ConstraintViolationException.class, () -> vehicleService.createVehicle(nullStatus));
    }

    @Test
    @DisplayName("Database uniqueness violation during flush is translated to DuplicateVehicleNumberException")
    void createVehicle_whenUniqueConstraintViolated_translatesToDuplicateVehicleNumberException() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "SG-1001",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        when(vehicleRepository.existsByVehicleNumberIgnoreCaseAndUserEmailIgnoreCase("SG-1001", SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(false);

        DataIntegrityViolationException dbException = new DataIntegrityViolationException(
                "could not execute statement",
                new org.hibernate.exception.ConstraintViolationException(
                        "ERROR: duplicate key value violates unique constraint \"uk_vehicles_vehicle_number\"",
                        new SQLException("duplicate key", "23505"),
                        "uk_vehicles_vehicle_number"
                )
        );

        when(vehicleRepository.saveAndFlush(any(Vehicle.class))).thenThrow(dbException);

        DuplicateVehicleNumberException ex = assertThrows(DuplicateVehicleNumberException.class,
                () -> vehicleService.createVehicle(request));

        assertEquals("A vehicle with this Vehicle Number already exists.", ex.getMessage());
        assertSame(dbException, ex.getCause());
    }

    @Test
    @DisplayName("Unrelated persistence failure is not mislabeled as duplicate and is rethrown")
    void createVehicle_whenUnrelatedDataIntegrityViolation_rethrowsOriginalException() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "SG-1001",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        when(vehicleRepository.existsByVehicleNumberIgnoreCaseAndUserEmailIgnoreCase("SG-1001", SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(false);

        DataIntegrityViolationException unrelatedException = new DataIntegrityViolationException(
                "check constraint violated",
                new org.hibernate.exception.ConstraintViolationException(
                        "ERROR: check constraint violated",
                        new SQLException("check constraint", "23514"),
                        "chk_vehicles_status"
                )
        );

        when(vehicleRepository.saveAndFlush(any(Vehicle.class))).thenThrow(unrelatedException);

        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class,
                () -> vehicleService.createVehicle(request));

        assertSame(unrelatedException, thrown);
    }

    // --- Update Vehicle Tests ---

    @Test
    @DisplayName("Updating a vehicle while keeping its own Vehicle Number succeeds and preserves ID")
    void updateVehicle_keepingSameVehicleNumber_succeedsAndPreservesId() {
        Long vehicleId = 42L;
        Vehicle existing = new Vehicle(vehicleId, "SG-1001", "Stark Industries", "Sedan", "Tesla", "Model S", VehicleStatus.ACTIVE);

        UpdateVehicleRequest request = new UpdateVehicleRequest(
                "SG-1001",
                "Stark Industries Updated",
                "SUV",
                "Tesla",
                "Model X",
                VehicleStatus.MAINTENANCE
        );

        when(vehicleRepository.findByIdAndUserEmailIgnoreCase(vehicleId, SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(Optional.of(existing));
        when(vehicleRepository.existsByVehicleNumberIgnoreCaseAndUserEmailIgnoreCaseAndIdNot("SG-1001", SecurityUtils.DEFAULT_USER_EMAIL, vehicleId)).thenReturn(false);
        when(vehicleRepository.saveAndFlush(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse updated = vehicleService.updateVehicle(vehicleId, request);

        assertEquals(vehicleId, updated.id());
        assertEquals("SG-1001", updated.vehicleNumber());
        assertEquals("Stark Industries Updated", updated.company());
        assertEquals("SUV", updated.type());
        assertEquals("Tesla", updated.brand());
        assertEquals("Model X", updated.model());
        assertEquals(VehicleStatus.MAINTENANCE, updated.status());
    }

    @Test
    @DisplayName("Updating a vehicle to another existing vehicle's number throws DuplicateVehicleNumberException")
    void updateVehicle_changingToAnotherVehiclesNumber_throwsDuplicateVehicleNumberException() {
        Long vehicleId = 42L;
        Vehicle existing = new Vehicle(vehicleId, "SG-1001", "Stark Industries", "Sedan", "Tesla", "Model S", VehicleStatus.ACTIVE);

        UpdateVehicleRequest request = new UpdateVehicleRequest(
                "  sg-9999  ",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        when(vehicleRepository.findByIdAndUserEmailIgnoreCase(vehicleId, SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(Optional.of(existing));
        when(vehicleRepository.existsByVehicleNumberIgnoreCaseAndUserEmailIgnoreCaseAndIdNot("SG-9999", SecurityUtils.DEFAULT_USER_EMAIL, vehicleId)).thenReturn(true);

        DuplicateVehicleNumberException ex = assertThrows(DuplicateVehicleNumberException.class,
                () -> vehicleService.updateVehicle(vehicleId, request));

        assertEquals("A vehicle with this Vehicle Number already exists.", ex.getMessage());
        verify(vehicleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Updating an unknown vehicle ID throws VehicleNotFoundException")
    void updateVehicle_withUnknownId_throwsVehicleNotFoundException() {
        Long unknownId = 999L;
        UpdateVehicleRequest request = new UpdateVehicleRequest(
                "SG-1001", "Stark Industries", "Sedan", "Tesla", "Model S", VehicleStatus.ACTIVE);

        when(vehicleRepository.findByIdAndUserEmailIgnoreCase(unknownId, SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.updateVehicle(unknownId, request));
    }

    // --- Retrieval and Deletion Tests ---

    @Test
    @DisplayName("Retrieving existing vehicle by ID returns response DTO")
    void getVehicleById_withExistingId_returnsDto() {
        Vehicle vehicle = new Vehicle(1L, "SG-1001", "Stark Industries", "Sedan", "Tesla", "Model S", VehicleStatus.ACTIVE);
        when(vehicleRepository.findByIdAndUserEmailIgnoreCase(1L, SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(Optional.of(vehicle));

        VehicleResponse response = vehicleService.getVehicleById(1L);

        assertEquals(1L, response.id());
        assertEquals("SG-1001", response.vehicleNumber());
    }

    @Test
    @DisplayName("Retrieving unknown vehicle ID throws VehicleNotFoundException")
    void getVehicleById_withUnknownId_throwsVehicleNotFoundException() {
        when(vehicleRepository.findByIdAndUserEmailIgnoreCase(999L, SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.getVehicleById(999L));
    }

    @Test
    @DisplayName("Listing all vehicles returns list of mapped DTOs")
    void getAllVehicles_returnsListOfMappedDtos() {
        List<Vehicle> list = List.of(
                new Vehicle(1L, "SG-1001", "Stark Industries", "Sedan", "Tesla", "Model S", VehicleStatus.ACTIVE),
                new Vehicle(2L, "SG-1002", "Stark Logistics", "Van", "Ford", "Transit", VehicleStatus.MAINTENANCE)
        );
        when(vehicleRepository.findAllByUserEmailIgnoreCase(SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(list);

        List<VehicleResponse> responses = vehicleService.getAllVehicles();

        assertEquals(2, responses.size());
        assertEquals("SG-1001", responses.get(0).vehicleNumber());
        assertEquals("SG-1002", responses.get(1).vehicleNumber());
    }

    @Test
    @DisplayName("Deleting an existing vehicle deletes the record")
    void deleteVehicle_withExistingId_deletesVehicle() {
        Vehicle vehicle = new Vehicle(1L, "SG-1001", "Stark Industries", "Sedan", "Tesla", "Model S", VehicleStatus.ACTIVE);
        when(vehicleRepository.findByIdAndUserEmailIgnoreCase(1L, SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(Optional.of(vehicle));

        vehicleService.deleteVehicle(1L);

        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    @DisplayName("Deleting an unknown vehicle ID throws VehicleNotFoundException")
    void deleteVehicle_withUnknownId_throwsVehicleNotFoundException() {
        when(vehicleRepository.findByIdAndUserEmailIgnoreCase(999L, SecurityUtils.DEFAULT_USER_EMAIL)).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.deleteVehicle(999L));
        verify(vehicleRepository, never()).delete(any());
    }
}
