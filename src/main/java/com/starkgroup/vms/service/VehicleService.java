package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.PageResponse;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.dto.VehicleResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VehicleService {

    VehicleResponse createVehicle(@Valid @NotNull CreateVehicleRequest request);

    VehicleResponse getVehicleById(@NotNull Long id);

    List<VehicleResponse> getAllVehicles();

    List<String> getVehicleCompanies();

    PageResponse<VehicleResponse> searchVehicles(String search, VehicleStatus status, String company, Pageable pageable);

    VehicleResponse updateVehicle(@NotNull Long id, @Valid @NotNull UpdateVehicleRequest request);

    VehicleResponse updateVehicleStatus(@NotNull Long id, @NotNull VehicleStatus status);

    VehicleResponse assignDriver(@NotNull Long vehicleId, @NotNull Long driverId);

    VehicleResponse unassignDriver(@NotNull Long vehicleId);

    void deleteVehicle(@NotNull Long id);
}
