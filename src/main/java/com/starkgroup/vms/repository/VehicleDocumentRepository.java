package com.starkgroup.vms.repository;

import com.starkgroup.vms.domain.VehicleDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findByVehicleIdOrderByExpiryDateAsc(Long vehicleId);

    List<VehicleDocument> findByVehicleId(Long vehicleId);

    // User-isolated methods
    @Query("SELECT d FROM VehicleDocument d WHERE LOWER(d.vehicle.userEmail) = LOWER(:userEmail) ORDER BY d.expiryDate ASC")
    List<VehicleDocument> findAllByUserEmailOrderByExpiryDateAsc(@Param("userEmail") String userEmail);

    @Query("SELECT COUNT(d) FROM VehicleDocument d WHERE LOWER(d.vehicle.userEmail) = LOWER(:userEmail)")
    long countByUserEmail(@Param("userEmail") String userEmail);

    @Query("SELECT COUNT(d) FROM VehicleDocument d WHERE LOWER(d.vehicle.userEmail) = LOWER(:userEmail) AND d.expiryDate < :date")
    long countExpiredByUser(@Param("userEmail") String userEmail, @Param("date") LocalDate date);

    @Query("SELECT COUNT(d) FROM VehicleDocument d WHERE LOWER(d.vehicle.userEmail) = LOWER(:userEmail) AND d.expiryDate BETWEEN :start AND :end")
    long countExpiringSoonByUser(@Param("userEmail") String userEmail, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT d FROM VehicleDocument d WHERE LOWER(d.vehicle.userEmail) = LOWER(:userEmail) AND d.expiryDate < :date ORDER BY d.expiryDate ASC")
    List<VehicleDocument> findExpiredByUser(@Param("userEmail") String userEmail, @Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT d FROM VehicleDocument d WHERE LOWER(d.vehicle.userEmail) = LOWER(:userEmail) AND d.expiryDate BETWEEN :start AND :end ORDER BY d.expiryDate ASC")
    List<VehicleDocument> findExpiringSoonByUser(@Param("userEmail") String userEmail, @Param("start") LocalDate start, @Param("end") LocalDate end, Pageable pageable);

    // Global fallbacks
    long countByExpiryDateBefore(LocalDate date);

    long countByExpiryDateBetween(LocalDate start, LocalDate end);

    List<VehicleDocument> findTop10ByExpiryDateBeforeOrderByExpiryDateAsc(LocalDate date);

    List<VehicleDocument> findTop10ByExpiryDateBetweenOrderByExpiryDateAsc(LocalDate start, LocalDate end);
}
