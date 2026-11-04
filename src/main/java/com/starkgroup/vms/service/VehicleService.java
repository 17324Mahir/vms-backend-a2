package com.starkgroup.vms.service;

import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.dto.VehicleResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface VehicleService {

    VehicleResponse createVehicle(@Valid @NotNull CreateVehicleRequest request);

    VehicleResponse getVehicleById(@NotNull Long id);

    List<VehicleResponse> getAllVehicles();

    VehicleResponse updateVehicle(@NotNull Long id, @Valid @NotNull UpdateVehicleRequest request);

    void deleteVehicle(@NotNull Long id);
}
