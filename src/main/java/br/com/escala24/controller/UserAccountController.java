package br.com.escala24.controller;

import java.security.Principal;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.escala24.dto.AuthenticatedUserResponse;
import br.com.escala24.dto.PasswordChangeRequest;
import br.com.escala24.entity.User;
import br.com.escala24.repository.UserRepository;
import br.com.escala24.service.PasswordChangeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me")
public class UserAccountController {

    private final PasswordChangeService passwordChangeService;
    private final UserRepository userRepository;

    public UserAccountController(
            PasswordChangeService passwordChangeService,
            UserRepository userRepository
    ) {
        this.passwordChangeService = passwordChangeService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<AuthenticatedUserResponse> getCurrentUser(
            Principal principal
    ) {
        String normalizedEmail = principal
                .getName()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuário autenticado não encontrado"
                        )
                );

        AuthenticatedUserResponse response =
                new AuthenticatedUserResponse(
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.isMustChangePassword()
                );

        return ResponseEntity.ok(response);
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