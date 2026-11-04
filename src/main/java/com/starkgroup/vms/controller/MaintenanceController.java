package com.starkgroup.vms.controller;

import com.starkgroup.vms.dto.CreateMaintenanceRequest;
import com.starkgroup.vms.dto.MaintenanceResponse;
import com.starkgroup.vms.dto.UpdateMaintenanceRequest;
import com.starkgroup.vms.service.MaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/maintenance")
@Validated
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping
    public ResponseEntity<MaintenanceResponse> recordMaintenance(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId,
            @Valid @RequestBody CreateMaintenanceRequest request) {
        MaintenanceResponse created = maintenanceService.recordMaintenance(vehicleId, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceResponse>> getMaintenanceHistory(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceHistoryForVehicle(vehicleId));
    }

    @GetMapping("/{mId}")
    public ResponseEntity<MaintenanceResponse> getMaintenanceById(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId,
            @PathVariable("mId") @Positive(message = "Maintenance ID must be a positive number") Long mId) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceById(mId));
    }

    @PutMapping("/{mId}")
    public ResponseEntity<MaintenanceResponse> updateMaintenance(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId,
            @PathVariable("mId") @Positive(message = "Maintenance ID must be a positive number") Long mId,
            @Valid @RequestBody UpdateMaintenanceRequest request) {
        return ResponseEntity.ok(maintenanceService.updateMaintenance(vehicleId, mId, request));
    }

    @DeleteMapping("/{mId}")
    public ResponseEntity<Void> deleteMaintenance(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId,
            @PathVariable("mId") @Positive(message = "Maintenance ID must be a positive number") Long mId) {
        maintenanceService.deleteMaintenance(vehicleId, mId);
        return ResponseEntity.noContent().build();
    }
}

