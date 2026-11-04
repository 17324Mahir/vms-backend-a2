package com.starkgroup.vms.repository;

import com.starkgroup.vms.domain.Driver;
import com.starkgroup.vms.domain.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, Long id);

    Optional<Driver> findByLicenseNumberIgnoreCase(String licenseNumber);

    List<Driver> findByStatus(DriverStatus status);

    long countByStatus(DriverStatus status);

    long countByLicenseExpiryDateBefore(LocalDate date);

    long countByLicenseExpiryDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT d FROM Driver d WHERE " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(COALESCE(d.email, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Driver> searchDrivers(@Param("search") String search,
                               @Param("status") DriverStatus status,
                               Pageable pageable);
}

