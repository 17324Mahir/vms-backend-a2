package com.starkgroup.vms.service;

import com.starkgroup.vms.dto.CreateDocumentRequest;
import com.starkgroup.vms.dto.DocumentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface VehicleDocumentService {

    DocumentResponse uploadDocument(@NotNull Long vehicleId, @Valid @NotNull CreateDocumentRequest request);

    DocumentResponse updateDocument(@NotNull Long vehicleId, @NotNull Long documentId, @Valid @NotNull CreateDocumentRequest request);

    List<DocumentResponse> getDocumentsForVehicle(@NotNull Long vehicleId);

    List<DocumentResponse> getAllDocumentsForCurrentUser();

    DocumentResponse getDocumentById(@NotNull Long documentId);

    void deleteDocument(@NotNull Long vehicleId, @NotNull Long documentId);

    void deleteDocumentById(@NotNull Long documentId);

    long countExpiredDocuments();

    long countExpiringSoonDocuments(int days);

    List<DocumentResponse> getUpcomingExpiries(int limit);

    long countExpiredDocumentsForUser(String userEmail);

    long countExpiringSoonDocumentsForUser(String userEmail, int days);

    List<DocumentResponse> getUpcomingExpiriesForUser(String userEmail, int limit);
}

