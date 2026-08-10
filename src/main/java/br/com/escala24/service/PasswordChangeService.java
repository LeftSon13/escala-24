package br.com.escala24.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import br.com.escala24.dto.PasswordChangeRequest;
import br.com.escala24.entity.User;
import br.com.escala24.exception.InvalidCurrentPasswordException;
import br.com.escala24.exception.PasswordConfirmationMismatchException;
import br.com.escala24.exception.PasswordReuseException;
import br.com.escala24.repository.UserRepository;
import jakarta.validation.Valid;

@Service
@Validated
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordChangeService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(
            String authenticatedEmail,
            @Valid PasswordChangeRequest request
    ) {
        String normalizedEmail = authenticatedEmail
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Usuário autenticado não encontrado"
                        )
                );

        validateCurrentPassword(user, request);
        validateConfirmation(request);
        validatePasswordReuse(user, request);

        user.setPassword(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );
        user.setMustChangePassword(false);
    }

    private void validateCurrentPassword(
            User user,
            PasswordChangeRequest request
    ) {
        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword()
        )) {
            throw new InvalidCurrentPasswordException();
        }
    }

    private void validateConfirmation(
            PasswordChangeRequest request
    ) {
        if (!request.newPassword().equals(
                request.newPasswordConfirmation()
        )) {
            throw new PasswordConfirmationMismatchException();
        }
    }

    private void validatePasswordReuse(
            User user,
            PasswordChangeRequest request
    ) {
        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPassword()
        )) {
            throw new PasswordReuseException();
        }
    }
}