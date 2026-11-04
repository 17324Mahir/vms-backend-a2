package com.starkgroup.vms.controller;

import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.dto.VehicleResponse;
import com.starkgroup.vms.service.VehicleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@Validated
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        VehicleResponse created = vehicleService.createVehicle(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        List<VehicleResponse> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(
            @PathVariable("id") @Positive(message = "Vehicle ID must be a positive number") Long id) {
        VehicleResponse vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable("id") @Positive(message = "Vehicle ID must be a positive number") Long id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        VehicleResponse updated = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable("id") @Positive(message = "Vehicle ID must be a positive number") Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
