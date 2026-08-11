package br.com.escala24.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HolidayResponse(
        Long id,
        LocalDate date,
        String name,
        LocalDateTime createdAt
) {
}