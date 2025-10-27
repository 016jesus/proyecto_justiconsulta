package com.justiconsulta.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalProcessResponseDto {
    private String legalProcessId;
    private OffsetDateTime lastActionDate;
    private OffsetDateTime createdAt;
}

