package com.starkgroup.vms.dto;

import com.starkgroup.vms.domain.DocumentType;
import com.starkgroup.vms.domain.VehicleDocument;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class DocumentResponse {

    private Long id;
    private Long vehicleId;
    private String vehicleNumber;
    private DocumentType documentType;
    private String documentNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String fileName;
    private String fileType;
    private boolean hasFileData;
    private String fileData;
    private String notes;
    private String expiryStatus; // EXPIRED, EXPIRING_SOON, VALID
    private long daysUntilExpiry;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public DocumentResponse() {
    }

    public static DocumentResponse fromEntity(VehicleDocument doc) {
        return fromEntity(doc, false);
    }

    public static DocumentResponse fromEntity(VehicleDocument doc, boolean includeFileData) {
        if (doc == null) return null;
        DocumentResponse resp = new DocumentResponse();
        resp.setId(doc.getId());
        if (doc.getVehicle() != null) {
            resp.setVehicleId(doc.getVehicle().getId());
            resp.setVehicleNumber(doc.getVehicle().getVehicleNumber());
        }
        resp.setDocumentType(doc.getDocumentType());
        resp.setDocumentNumber(doc.getDocumentNumber());
        resp.setIssueDate(doc.getIssueDate());
        resp.setExpiryDate(doc.getExpiryDate());
        resp.setFileName(doc.getFileName());
        resp.setFileType(doc.getFileType());
        resp.setHasFileData(doc.getFileData() != null && !doc.getFileData().isEmpty());
        if (includeFileData) {
            resp.setFileData(doc.getFileData());
        }
        resp.setNotes(doc.getNotes());
        resp.setCreatedAt(doc.getCreatedAt());
        resp.setUpdatedAt(doc.getUpdatedAt());

        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, doc.getExpiryDate());
        resp.setDaysUntilExpiry(days);
        if (days < 0) {
            resp.setExpiryStatus("EXPIRED");
        } else if (days <= 30) {
            resp.setExpiryStatus("EXPIRING_SOON");
        } else {
            resp.setExpiryStatus("VALID");
        }

        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public boolean isHasFileData() {
        return hasFileData;
    }

    public void setHasFileData(boolean hasFileData) {
        this.hasFileData = hasFileData;
    }

    public String getFileData() {
        return fileData;
    }

    public void setFileData(String fileData) {
        this.fileData = fileData;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getExpiryStatus() {
        return expiryStatus;
    }

    public void setExpiryStatus(String expiryStatus) {
        this.expiryStatus = expiryStatus;
    }

    public long getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public void setDaysUntilExpiry(long daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
