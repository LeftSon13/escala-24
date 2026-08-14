package br.com.escala24.service;

import java.util.Locale;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.AuthenticatedUserResponse;
import br.com.escala24.entity.User;
import br.com.escala24.repository.UserRepository;

@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse getByEmail(
            String email
    ) {
        String normalizedEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuário autenticado não encontrado"
                        )
                );

        return new AuthenticatedUserResponse(
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isMustChangePassword()
        );
    }
}