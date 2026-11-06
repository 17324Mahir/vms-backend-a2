package com.starkgroup.vms.controller;

import com.starkgroup.vms.dto.DocumentResponse;
import com.starkgroup.vms.service.VehicleDocumentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Validated
public class FleetDocumentController {

    private final VehicleDocumentService documentService;

    public FleetDocumentController(VehicleDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocumentsForCurrentUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocumentFile(@PathVariable("id") Long id) {
        DocumentResponse doc = documentService.getDocumentById(id);
        if (doc.getFileData() == null || doc.getFileData().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Base64.getDecoder().decode(doc.getFileData());
        String filename = doc.getFileName() != null ? doc.getFileName() : "document-" + id;
        String contentType = "application/octet-stream";
        if (doc.getFileType() != null) {
            String ext = doc.getFileType().toLowerCase();
            if (ext.equals("pdf")) contentType = "application/pdf";
            else if (ext.equals("png")) contentType = "image/png";
            else if (ext.equals("jpg") || ext.equals("jpeg")) contentType = "image/jpeg";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable("id") Long id) {
        documentService.deleteDocumentById(id);
        return ResponseEntity.noContent().build();
    }
}

