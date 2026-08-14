package br.com.escala24.dto;

import br.com.escala24.entity.Role;

public record AuthenticatedUserResponse(
        String name,
        String email,
        Role role,
        boolean mustChangePassword
) {
}