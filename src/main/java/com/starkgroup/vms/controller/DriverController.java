package com.starkgroup.vms.controller;

import com.starkgroup.vms.domain.DriverStatus;
import com.starkgroup.vms.dto.CreateDriverRequest;
import com.starkgroup.vms.dto.DriverResponse;
import com.starkgroup.vms.dto.DriverSummaryDto;
import com.starkgroup.vms.dto.PageResponse;
import com.starkgroup.vms.dto.UpdateDriverRequest;
import com.starkgroup.vms.service.DriverService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/drivers")
@Validated
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody CreateDriverRequest request) {
        DriverResponse created = driverService.createDriver(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<?> getDrivers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DriverStatus status,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        if (page == null && size == null && search == null && status == null) {
            List<DriverResponse> drivers = driverService.getAllDrivers();
            return ResponseEntity.ok(drivers);
        }

        int pageNum = page != null ? Math.max(page, 0) : 0;
        int pageSize = size != null ? Math.max(size, 1) : 10;
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        PageResponse<DriverResponse> result = driverService.searchDrivers(search, status, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/available")
    public ResponseEntity<List<DriverSummaryDto>> getAvailableDrivers() {
        return ResponseEntity.ok(driverService.getAvailableDrivers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(
            @PathVariable("id") @Positive(message = "Driver ID must be a positive number") Long id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable("id") @Positive(message = "Driver ID must be a positive number") Long id,
            @Valid @RequestBody UpdateDriverRequest request) {
        return ResponseEntity.ok(driverService.updateDriver(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(
            @PathVariable("id") @Positive(message = "Driver ID must be a positive number") Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }
}

