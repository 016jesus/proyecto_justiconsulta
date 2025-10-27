package com.justiconsulta.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryResponseDto {
    private UUID id;
    private String legalProcessId;
    private UUID activitySeriesId;
    private OffsetDateTime date;
    private String result;
    private OffsetDateTime createdAt;
    private String userDocumentNumber;
}

