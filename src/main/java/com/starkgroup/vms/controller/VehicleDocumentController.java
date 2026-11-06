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

    @PutMapping("/{docId}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId,
            @PathVariable("docId") @Positive(message = "Document ID must be a positive number") Long docId,
            @Valid @RequestBody CreateDocumentRequest request) {
        DocumentResponse updated = documentService.updateDocument(vehicleId, docId, request);
        return ResponseEntity.ok(updated);
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

    @GetMapping("/{docId}/download")
    public ResponseEntity<byte[]> downloadDocumentFile(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId,
            @PathVariable("docId") @Positive(message = "Document ID must be a positive number") Long docId) {
        DocumentResponse doc = documentService.getDocumentById(docId);
        if (doc.getFileData() == null || doc.getFileData().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = java.util.Base64.getDecoder().decode(doc.getFileData());
        String filename = doc.getFileName() != null ? doc.getFileName() : "document-" + docId;
        String contentType = "application/octet-stream";
        if (doc.getFileType() != null) {
            String ext = doc.getFileType().toLowerCase();
            if (ext.equals("pdf")) contentType = "application/pdf";
            else if (ext.equals("png")) contentType = "image/png";
            else if (ext.equals("jpg") || ext.equals("jpeg")) contentType = "image/jpeg";
        }
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable("vehicleId") @Positive(message = "Vehicle ID must be a positive number") Long vehicleId,
            @PathVariable("docId") @Positive(message = "Document ID must be a positive number") Long docId) {
        documentService.deleteDocument(vehicleId, docId);
        return ResponseEntity.noContent().build();
    }
}

