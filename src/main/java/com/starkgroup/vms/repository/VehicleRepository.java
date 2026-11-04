package com.starkgroup.vms.repository;

import com.starkgroup.vms.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByVehicleNumber(String vehicleNumber);

    boolean existsByVehicleNumberAndIdNot(String vehicleNumber, Long id);

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);
}
