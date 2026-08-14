package br.com.escala24.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.escala24.dto.AuthenticatedUserResponse;
import br.com.escala24.dto.PasswordChangeRequest;
import br.com.escala24.service.AuthenticatedUserService;
import br.com.escala24.service.PasswordChangeService;
import br.com.escala24.service.SessionAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me")
public class UserAccountController {

    private final PasswordChangeService passwordChangeService;
    private final AuthenticatedUserService authenticatedUserService;
    private final SessionAuthenticationService
            sessionAuthenticationService;

    public UserAccountController(
            PasswordChangeService passwordChangeService,
            AuthenticatedUserService authenticatedUserService,
            SessionAuthenticationService sessionAuthenticationService
    ) {
        this.passwordChangeService = passwordChangeService;
        this.authenticatedUserService = authenticatedUserService;
        this.sessionAuthenticationService =
                sessionAuthenticationService;
    }

    @GetMapping
    public ResponseEntity<AuthenticatedUserResponse> getCurrentUser(
            Principal principal
    ) {
        AuthenticatedUserResponse response =
                authenticatedUserService.getByEmail(
                        principal.getName()
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            Principal principal,
            @Valid @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        passwordChangeService.changePassword(
                principal.getName(),
                request
        );

        sessionAuthenticationService.authenticateAndStore(
                principal.getName(),
                request.newPassword(),
                httpRequest,
                httpResponse
        );

        return ResponseEntity.noContent().build();
    }
}