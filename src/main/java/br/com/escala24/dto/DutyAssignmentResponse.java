package br.com.escala24.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.escala24.entity.DayType;

public record DutyAssignmentResponse(
        Long id,
        LocalDate dutyDate,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        DayType dayType,
        Long firefighterId,
        String firefighterName,
        String firefighterRegistration
) {
}