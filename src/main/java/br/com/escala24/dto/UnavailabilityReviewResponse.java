package br.com.escala24.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.escala24.entity.UnavailabilityStatus;
import br.com.escala24.entity.UnavailabilityType;

public record UnavailabilityReviewResponse(
        Long id,
        Long firefighterId,
        String firefighterName,
        UnavailabilityType type,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        UnavailabilityStatus status,
        LocalDateTime requestedAt
) {
}