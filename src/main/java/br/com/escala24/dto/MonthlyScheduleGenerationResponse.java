package br.com.escala24.dto;

import java.time.LocalDateTime;
import java.util.List;

import br.com.escala24.entity.ScheduleStatus;

public record MonthlyScheduleGenerationResponse(
        Long id,
        int year,
        int month,
        ScheduleStatus status,
        LocalDateTime createdAt,
        List<DutyAssignmentResponse> assignments
) {
}