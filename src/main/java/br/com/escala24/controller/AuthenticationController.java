package br.com.escala24.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.escala24.dto.AuthenticatedUserResponse;
import br.com.escala24.dto.LoginRequest;
import br.com.escala24.service.AuthenticatedUserService;
import br.com.escala24.service.SessionAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final SessionAuthenticationService
            sessionAuthenticationService;

    private final AuthenticatedUserService
            authenticatedUserService;

    public AuthenticationController(
            SessionAuthenticationService sessionAuthenticationService,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.sessionAuthenticationService =
                sessionAuthenticationService;

        this.authenticatedUserService =
                authenticatedUserService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticatedUserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        sessionAuthenticationService.authenticateAndStore(
                request.email(),
                request.password(),
                httpRequest,
                httpResponse
        );

        AuthenticatedUserResponse response =
                authenticatedUserService.getByEmail(
                        request.email()
                );

        return ResponseEntity.ok(response);
    }
}