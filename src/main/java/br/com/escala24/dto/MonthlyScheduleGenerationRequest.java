package br.com.escala24.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record MonthlyScheduleGenerationRequest(

        @Positive(message = "O ano deve ser positivo")
        int year,

        @Min(
                value = 1,
                message = "O mês deve estar entre 1 e 12"
        )
        @Max(
                value = 12,
                message = "O mês deve estar entre 1 e 12"
        )
        int month
) {
}