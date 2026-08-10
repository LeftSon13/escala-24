package br.com.escala24.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.escala24.dto.PasswordChangeRequest;
import br.com.escala24.service.PasswordChangeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me")
public class UserAccountController {

    private final PasswordChangeService passwordChangeService;

    public UserAccountController(
            PasswordChangeService passwordChangeService
    ) {
        this.passwordChangeService = passwordChangeService;
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            Principal principal,
            @Valid @RequestBody
            PasswordChangeRequest request
    ) {
        passwordChangeService.changePassword(
                principal.getName(),
                request
        );

        return ResponseEntity.noContent().build();
    }
}