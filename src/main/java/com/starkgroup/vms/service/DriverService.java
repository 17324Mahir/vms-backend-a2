package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.DriverStatus;
import com.starkgroup.vms.dto.CreateDriverRequest;
import com.starkgroup.vms.dto.DriverResponse;
import com.starkgroup.vms.dto.DriverSummaryDto;
import com.starkgroup.vms.dto.PageResponse;
import com.starkgroup.vms.dto.UpdateDriverRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DriverService {

    DriverResponse createDriver(@Valid @NotNull CreateDriverRequest request);

    DriverResponse getDriverById(@NotNull Long id);

    List<DriverResponse> getAllDrivers();

    List<DriverSummaryDto> getAvailableDrivers();

    PageResponse<DriverResponse> searchDrivers(String search, DriverStatus status, Pageable pageable);

    DriverResponse updateDriver(@NotNull Long id, @Valid @NotNull UpdateDriverRequest request);

    void deleteDriver(@NotNull Long id);
}

