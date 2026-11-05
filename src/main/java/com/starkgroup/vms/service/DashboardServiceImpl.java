package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.DriverStatus;
import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.DashboardStatsResponse;
import com.starkgroup.vms.dto.DocumentResponse;
import com.starkgroup.vms.dto.MaintenanceResponse;
import com.starkgroup.vms.repository.DriverRepository;
import com.starkgroup.vms.repository.VehicleDocumentRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.util.SecurityUtils;
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
        String userEmail = SecurityUtils.getCurrentUserEmail();

        long totalVehicles = vehicleRepository.countByUserEmailIgnoreCase(userEmail);
        long activeVehicles = vehicleRepository.countByUserEmailIgnoreCaseAndStatus(userEmail, VehicleStatus.ACTIVE);
        long maintenanceVehicles = vehicleRepository.countByUserEmailIgnoreCaseAndStatus(userEmail, VehicleStatus.MAINTENANCE);
        long inactiveVehicles = vehicleRepository.countByUserEmailIgnoreCaseAndStatus(userEmail, VehicleStatus.INACTIVE);

        long totalDrivers = driverRepository.countByUserEmailIgnoreCase(userEmail);
        long availableDrivers = driverRepository.countByUserEmailIgnoreCaseAndStatus(userEmail, DriverStatus.AVAILABLE);
        long assignedDrivers = driverRepository.countByUserEmailIgnoreCaseAndStatus(userEmail, DriverStatus.ASSIGNED);

        long totalDocuments = documentRepository.countByUserEmail(userEmail);
        long expiredDocuments = documentService.countExpiredDocumentsForUser(userEmail);
        long expiringSoonDocuments = documentService.countExpiringSoonDocumentsForUser(userEmail, 30);

        BigDecimal totalMaintenanceCost = maintenanceService.getTotalMaintenanceCostForUser(userEmail);
        long totalMaintenanceRecords = maintenanceService.countTotalMaintenanceRecordsForUser(userEmail);

        List<DocumentResponse> upcomingExpiries = documentService.getUpcomingExpiriesForUser(userEmail, 5);
        List<MaintenanceResponse> recentMaintenance = maintenanceService.getRecentMaintenanceForUser(userEmail, 5);

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
                totalMaintenanceCost != null ? totalMaintenanceCost : BigDecimal.ZERO,
                totalMaintenanceRecords,
                upcomingExpiries,
                recentMaintenance
        );
    }
}

