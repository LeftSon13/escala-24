package br.com.escala24.dto;

import java.time.LocalDate;

import br.com.escala24.entity.UnavailabilityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UnavailabilityRequest(

        @NotNull(message = "O tipo é obrigatório")
        UnavailabilityType type,

        @NotNull(message = "A data inicial é obrigatória")
        LocalDate startDate,

        @NotNull(message = "A data final é obrigatória")
        LocalDate endDate,

        @Size(
                max = 500,
                message = "A justificativa deve ter no máximo 500 caracteres"
        )
        String reason
) {
}