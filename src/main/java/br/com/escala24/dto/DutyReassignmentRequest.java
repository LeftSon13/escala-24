package br.com.escala24.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DutyReassignmentRequest(

        @NotNull(message = "O bombeiro substituto é obrigatório")
        @Positive(message = "O identificador do bombeiro deve ser positivo")
        Long firefighterId

) {
}