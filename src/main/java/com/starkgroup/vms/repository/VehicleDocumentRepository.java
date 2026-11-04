package com.starkgroup.vms.repository;

import com.starkgroup.vms.domain.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findByVehicleIdOrderByExpiryDateAsc(Long vehicleId);

    List<VehicleDocument> findByVehicleId(Long vehicleId);

    long countByExpiryDateBefore(LocalDate date);

    long countByExpiryDateBetween(LocalDate start, LocalDate end);

    List<VehicleDocument> findTop10ByExpiryDateBeforeOrderByExpiryDateAsc(LocalDate date);

    List<VehicleDocument> findTop10ByExpiryDateBetweenOrderByExpiryDateAsc(LocalDate start, LocalDate end);
}
