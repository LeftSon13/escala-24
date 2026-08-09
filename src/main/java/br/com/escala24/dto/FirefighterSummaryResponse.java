package br.com.escala24.dto;

public record FirefighterSummaryResponse(
        Long firefighterId,
        Long userId,
        String name,
        String email,
        String registration,
        String phone,
        boolean active
) {
}