package br.com.escala24.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HolidayRequest(

        @NotNull
        LocalDate date,

        @NotBlank
        @Size(max = 150)
        String name

) {
}