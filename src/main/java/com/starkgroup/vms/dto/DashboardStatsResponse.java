package com.starkgroup.vms.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsResponse(
    long totalVehicles,
    long activeVehicles,
    long maintenanceVehicles,
    long inactiveVehicles,
    long totalDrivers,
    long availableDrivers,
    long assignedDrivers,
    long totalDocuments,
    long expiredDocuments,
    long expiringSoonDocuments,
    BigDecimal totalMaintenanceCost,
    long totalMaintenanceRecords,
    List<DocumentResponse> upcomingExpiries,
    List<MaintenanceResponse> recentMaintenance
) {
}

