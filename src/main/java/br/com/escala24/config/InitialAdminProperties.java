package br.com.escala24.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "escala24.bootstrap.admin"
)
public record InitialAdminProperties(
        boolean enabled,
        String name,
        String email,
        String password
) {
}