package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.DriverStatus;
import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.DashboardStatsResponse;
import com.starkgroup.vms.dto.DocumentResponse;
import com.starkgroup.vms.dto.MaintenanceResponse;
import com.starkgroup.vms.repository.DriverRepository;
import com.starkgroup.vms.repository.VehicleDocumentRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final VehicleDocumentRepository documentRepository;
    private final VehicleDocumentService documentService;
    private final MaintenanceService maintenanceService;

    public DashboardServiceImpl(VehicleRepository vehicleRepository,
                                DriverRepository driverRepository,
                                VehicleDocumentRepository documentRepository,
                                VehicleDocumentService documentService,
                                MaintenanceService maintenanceService) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.maintenanceService = maintenanceService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalVehicles = vehicleRepository.count();
        long activeVehicles = vehicleRepository.countByStatus(VehicleStatus.ACTIVE);
        long maintenanceVehicles = vehicleRepository.countByStatus(VehicleStatus.MAINTENANCE);
        long inactiveVehicles = vehicleRepository.countByStatus(VehicleStatus.INACTIVE);

        long totalDrivers = driverRepository.count();
        long availableDrivers = driverRepository.countByStatus(DriverStatus.AVAILABLE);
        long assignedDrivers = driverRepository.countByStatus(DriverStatus.ASSIGNED);

        long totalDocuments = documentRepository.count();
        long expiredDocuments = documentService.countExpiredDocuments();
        long expiringSoonDocuments = documentService.countExpiringSoonDocuments(30);

        BigDecimal totalMaintenanceCost = maintenanceService.getTotalMaintenanceCost();
        long totalMaintenanceRecords = maintenanceService.countTotalMaintenanceRecords();

        List<DocumentResponse> upcomingExpiries = documentService.getUpcomingExpiries(5);
        List<MaintenanceResponse> recentMaintenance = maintenanceService.getRecentMaintenance(5);

        return new DashboardStatsResponse(
                totalVehicles,
                activeVehicles,
                maintenanceVehicles,
                inactiveVehicles,
                totalDrivers,
                availableDrivers,
                assignedDrivers,
                totalDocuments,
                expiredDocuments,
                expiringSoonDocuments,
                totalMaintenanceCost,
                totalMaintenanceRecords,
                upcomingExpiries,
                recentMaintenance
        );
    }
}
