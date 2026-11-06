package com.starkgroup.vms.controller;

import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.PageResponse;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.dto.VehicleResponse;
import com.starkgroup.vms.service.VehicleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> getVehicles(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) String company,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        if (page == null && size == null && search == null && status == null && company == null) {
            List<VehicleResponse> vehicles = vehicleService.getAllVehicles();
            return ResponseEntity.ok(vehicles);
        }

        int pageNum = page != null ? Math.max(page, 0) : 0;
        int pageSize = size != null ? Math.max(size, 1) : 10;
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        if (!sortBy.equals("id")) {
            sort = sort.and(Sort.by("id"));
        }
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        PageResponse<VehicleResponse> result = vehicleService.searchVehicles(search, status, company, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/companies")
    public List<String> getVehicleCompanies() {
        return vehicleService.getVehicleCompanies();
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

    @PatchMapping("/{id}/status")
    public ResponseEntity<VehicleResponse> updateVehicleStatus(
            @PathVariable("id") @Positive(message = "Vehicle ID must be a positive number") Long id,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        VehicleStatus status = VehicleStatus.valueOf(statusStr.toUpperCase());
        VehicleResponse updated = vehicleService.updateVehicleStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/assign-driver/{driverId}")
    public ResponseEntity<VehicleResponse> assignDriver(
            @PathVariable("id") @Positive(message = "Vehicle ID must be a positive number") Long id,
            @PathVariable("driverId") @Positive(message = "Driver ID must be a positive number") Long driverId) {
        VehicleResponse updated = vehicleService.assignDriver(id, driverId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/unassign-driver")
    public ResponseEntity<VehicleResponse> unassignDriver(
            @PathVariable("id") @Positive(message = "Vehicle ID must be a positive number") Long id) {
        VehicleResponse updated = vehicleService.unassignDriver(id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable("id") @Positive(message = "Vehicle ID must be a positive number") Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
