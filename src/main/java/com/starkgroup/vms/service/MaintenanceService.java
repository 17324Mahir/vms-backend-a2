package com.starkgroup.vms.service;

import com.starkgroup.vms.dto.CreateMaintenanceRequest;
import com.starkgroup.vms.dto.MaintenanceResponse;
import com.starkgroup.vms.dto.UpdateMaintenanceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public interface MaintenanceService {

    MaintenanceResponse recordMaintenance(@NotNull Long vehicleId, @Valid @NotNull CreateMaintenanceRequest request);

    List<MaintenanceResponse> getMaintenanceHistoryForVehicle(@NotNull Long vehicleId);

    List<MaintenanceResponse> getAllMaintenanceForCurrentUser();

    MaintenanceResponse getMaintenanceById(@NotNull Long maintenanceId);

    MaintenanceResponse updateMaintenance(@NotNull Long vehicleId, @NotNull Long maintenanceId, @Valid @NotNull UpdateMaintenanceRequest request);

    MaintenanceResponse updateMaintenanceById(@NotNull Long maintenanceId, @Valid @NotNull UpdateMaintenanceRequest request);

    void deleteMaintenance(@NotNull Long vehicleId, @NotNull Long maintenanceId);

    void deleteMaintenanceById(@NotNull Long maintenanceId);

    BigDecimal getTotalMaintenanceCost();

    BigDecimal getTotalMaintenanceCostForVehicle(@NotNull Long vehicleId);

    long countTotalMaintenanceRecords();

    List<MaintenanceResponse> getRecentMaintenance(int limit);

    BigDecimal getTotalMaintenanceCostForUser(String userEmail);

    long countTotalMaintenanceRecordsForUser(String userEmail);

    List<MaintenanceResponse> getRecentMaintenanceForUser(String userEmail, int limit);
}

