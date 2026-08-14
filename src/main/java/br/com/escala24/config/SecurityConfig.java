package br.com.escala24.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import br.com.escala24.security.RestAccessDeniedHandler;
import br.com.escala24.security.RestAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )
                .httpBasic(basic ->
                        basic.authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                        .accessDeniedHandler(
                                accessDeniedHandler
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        )
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me"
                        )
                        .authenticated()
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/me/password"
                        )
                        .authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/holidays/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "FIREFIGHTER"
                        )
                        .requestMatchers(
                                "/api/holidays/**"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/unavailabilities/pending"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/unavailabilities/*/approval",
                                "/api/unavailabilities/*/rejection"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/unavailabilities"
                        )
                        .hasRole("FIREFIGHTER")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/unavailabilities/me"
                        )
                        .hasRole("FIREFIGHTER")
                        .requestMatchers(
                                "/api/firefighters/**"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/monthly-schedules/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "FIREFIGHTER"
                        )
                        .requestMatchers(
                                "/api/monthly-schedules/**"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                "/api/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "FIREFIGHTER"
                        )
                        .anyRequest()
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}