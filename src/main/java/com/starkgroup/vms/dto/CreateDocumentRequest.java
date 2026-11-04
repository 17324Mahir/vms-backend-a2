package com.starkgroup.vms.dto;

import com.starkgroup.vms.domain.DocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class CreateDocumentRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @Size(max = 100, message = "Document number must not exceed 100 characters")
    private String documentNumber;

    private LocalDate issueDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;

    @Size(max = 100, message = "File type must not exceed 100 characters")
    private String fileType;

    private String fileData; // Base64 or URL

    private String notes;

    public CreateDocumentRequest() {
    }

    public CreateDocumentRequest(DocumentType documentType, String documentNumber, LocalDate issueDate,
                                 LocalDate expiryDate, String fileName, String fileType, String fileData, String notes) {
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileData = fileData;
        this.notes = notes;
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
}

