package br.com.escala24.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FirefighterRegistrationRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String temporaryPassword,

        @NotBlank
        @Size(max = 50)
        String registration,

        @NotBlank
        @Size(max = 20)
        String phone

) {
}