package com.starkgroup.vms.controller;

import com.starkgroup.vms.dto.CreateDocumentRequest;
import com.starkgroup.vms.dto.DocumentResponse;
import com.starkgroup.vms.service.VehicleDocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/documents")
@Validated
public class VehicleDocumentController {

    private final VehicleDocumentService documentService;

    public VehicleDocumentController(VehicleDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId,
            @Valid @RequestBody CreateDocumentRequest request) {
        DocumentResponse created = documentService.uploadDocument(vehicleId, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocumentsForVehicle(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId) {
        return ResponseEntity.ok(documentService.getDocumentsForVehicle(vehicleId));
    }

    @GetMapping("/{docId}")
    public ResponseEntity<DocumentResponse> getDocumentById(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId,
            @PathVariable("docId") @Positive(message = "Document ID must be a positive number") Long docId) {
        return ResponseEntity.ok(documentService.getDocumentById(docId));
    }

    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId,
            @PathVariable("docId") @Positive(message = "Document ID must be a positive number") Long docId) {
        documentService.deleteDocument(vehicleId, docId);
        return ResponseEntity.noContent().build();
    }
}
