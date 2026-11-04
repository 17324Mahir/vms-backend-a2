package com.starkgroup.vms.repository;

import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.domain.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByVehicleNumber(String vehicleNumber);

    boolean existsByVehicleNumberIgnoreCase(String vehicleNumber);

    boolean existsByVehicleNumberAndIdNot(String vehicleNumber, Long id);

    boolean existsByVehicleNumberIgnoreCaseAndIdNot(String vehicleNumber, Long id);

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    Optional<Vehicle> findByVehicleNumberIgnoreCase(String vehicleNumber);

    List<Vehicle> findByAssignedDriverId(Long driverId);

    long countByStatus(VehicleStatus status);

    @Query("SELECT v FROM Vehicle v LEFT JOIN v.assignedDriver d WHERE " +
           "(:status IS NULL OR v.status = :status) AND " +
           "(:company IS NULL OR :company = '' OR :company = 'All Companies' OR v.company = :company) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(v.vehicleNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.brand) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.company) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(d IS NOT NULL AND LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Vehicle> searchVehicles(@Param("search") String search,
                                 @Param("status") VehicleStatus status,
                                 @Param("company") String company,
                                 Pageable pageable);
}
