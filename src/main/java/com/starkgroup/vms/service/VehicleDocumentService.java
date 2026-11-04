package com.starkgroup.vms.service;

import com.starkgroup.vms.dto.CreateDocumentRequest;
import com.starkgroup.vms.dto.DocumentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface VehicleDocumentService {

    DocumentResponse uploadDocument(@NotNull Long vehicleId, @Valid @NotNull CreateDocumentRequest request);

    List<DocumentResponse> getDocumentsForVehicle(@NotNull Long vehicleId);

    DocumentResponse getDocumentById(@NotNull Long documentId);

    void deleteDocument(@NotNull Long vehicleId, @NotNull Long documentId);

    long countExpiredDocuments();

    long countExpiringSoonDocuments(int days);

    List<DocumentResponse> getUpcomingExpiries(int limit);
}
