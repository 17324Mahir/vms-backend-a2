package com.starkgroup.vms.controller;

import com.starkgroup.vms.dto.MaintenanceResponse;
import com.starkgroup.vms.service.MaintenanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
@Validated
public class FleetMaintenanceController {

    private final MaintenanceService maintenanceService;

    public FleetMaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceResponse>> getAllMaintenance() {
        return ResponseEntity.ok(maintenanceService.getAllMaintenanceForCurrentUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> getMaintenanceById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceById(id));
    }
}
