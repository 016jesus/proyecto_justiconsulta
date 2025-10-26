package com.justiconsulta.store.dto.request;

import jakarta.validation.constraints.NotBlank;

// DTO para recibir el documentNumber en el body
public class DocumentNumberRequest {
    @NotBlank
    private String documentNumber;
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
}