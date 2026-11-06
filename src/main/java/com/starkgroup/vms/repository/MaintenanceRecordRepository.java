package com.starkgroup.vms.repository;

import com.starkgroup.vms.domain.MaintenanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByVehicleIdOrderByMaintenanceDateDesc(Long vehicleId);

    Page<MaintenanceRecord> findByVehicleId(Long vehicleId, Pageable pageable);

    // User-isolated methods
    @Query("SELECT m FROM MaintenanceRecord m WHERE LOWER(m.vehicle.userEmail) = LOWER(:userEmail) ORDER BY m.maintenanceDate DESC")
    List<MaintenanceRecord> findAllByUserEmailOrderByMaintenanceDateDesc(@Param("userEmail") String userEmail);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m WHERE LOWER(m.vehicle.userEmail) = LOWER(:userEmail)")
    BigDecimal calculateTotalMaintenanceCostByUser(@Param("userEmail") String userEmail);

    @Query("SELECT COUNT(m) FROM MaintenanceRecord m WHERE LOWER(m.vehicle.userEmail) = LOWER(:userEmail)")
    long countByUserEmail(@Param("userEmail") String userEmail);

    @Query("SELECT m FROM MaintenanceRecord m WHERE LOWER(m.vehicle.userEmail) = LOWER(:userEmail) ORDER BY m.maintenanceDate DESC")
    List<MaintenanceRecord> findRecentByUser(@Param("userEmail") String userEmail, Pageable pageable);

    // Global fallbacks
    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m")
    BigDecimal calculateTotalMaintenanceCost();

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m WHERE m.vehicle.id = :vehicleId")
    BigDecimal calculateTotalMaintenanceCostForVehicle(@Param("vehicleId") Long vehicleId);

    long countByMaintenanceDateAfter(LocalDate date);

    List<MaintenanceRecord> findTop10ByOrderByMaintenanceDateDesc();
}
