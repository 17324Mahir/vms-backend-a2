package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.Vehicle;
import com.starkgroup.vms.domain.VehicleDocument;
import com.starkgroup.vms.dto.CreateDocumentRequest;
import com.starkgroup.vms.dto.DocumentResponse;
import com.starkgroup.vms.exception.ResourceNotFoundException;
import com.starkgroup.vms.exception.VehicleNotFoundException;
import com.starkgroup.vms.repository.VehicleDocumentRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Validated
public class VehicleDocumentServiceImpl implements VehicleDocumentService {

    private final VehicleDocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;
    private final Validator validator;

    public VehicleDocumentServiceImpl(VehicleDocumentRepository documentRepository, VehicleRepository vehicleRepository) {
        this(documentRepository, vehicleRepository, Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Autowired
    public VehicleDocumentServiceImpl(VehicleDocumentRepository documentRepository,
                                      VehicleRepository vehicleRepository,
                                      Validator validator) {
        this.documentRepository = documentRepository;
        this.vehicleRepository = vehicleRepository;
        this.validator = validator != null ? validator : Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(Long vehicleId, CreateDocumentRequest request) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        validate(request);

        VehicleDocument document = new VehicleDocument(
                vehicle,
                request.getDocumentType(),
                request.getDocumentNumber() != null ? request.getDocumentNumber().trim() : null,
                request.getIssueDate(),
                request.getExpiryDate(),
                request.getFileName() != null ? request.getFileName().trim() : null,
                request.getFileType() != null ? request.getFileType().trim() : null,
                request.getFileData(),
                request.getNotes() != null ? request.getNotes().trim() : null
        );

        VehicleDocument saved = documentRepository.save(document);
        return DocumentResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsForVehicle(Long vehicleId) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new VehicleNotFoundException(vehicleId);
        }

        return documentRepository.findByVehicleIdOrderByExpiryDateAsc(vehicleId).stream()
                .map(DocumentResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long documentId) {
        if (documentId == null) {
            throw new ResourceNotFoundException("Document ID must not be null");
        }
        VehicleDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        return DocumentResponse.fromEntity(doc);
    }

    @Override
    @Transactional
    public void deleteDocument(Long vehicleId, Long documentId) {
        if (vehicleId == null) {
            throw new VehicleNotFoundException("Vehicle ID must not be null");
        }
        if (documentId == null) {
            throw new ResourceNotFoundException("Document ID must not be null");
        }

        VehicleDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!doc.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Document does not belong to the specified vehicle");
        }

        documentRepository.delete(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public long countExpiredDocuments() {
        return documentRepository.countByExpiryDateBefore(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countExpiringSoonDocuments(int days) {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(days);
        return documentRepository.countByExpiryDateBetween(today, threshold);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getUpcomingExpiries(int limit) {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        List<VehicleDocument> expired = documentRepository.findTop10ByExpiryDateBeforeOrderByExpiryDateAsc(today);
        List<VehicleDocument> expiringSoon = documentRepository.findTop10ByExpiryDateBetweenOrderByExpiryDateAsc(today, threshold);

        List<DocumentResponse> result = new ArrayList<>();
        expired.forEach(d -> result.add(DocumentResponse.fromEntity(d)));
        expiringSoon.forEach(d -> result.add(DocumentResponse.fromEntity(d)));

        if (result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }

    private <T> void validate(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}

