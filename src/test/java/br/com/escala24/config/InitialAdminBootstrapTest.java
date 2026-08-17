package br.com.escala24.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;
import br.com.escala24.repository.UserRepository;

class InitialAdminBootstrapTest {

    private static final String VALID_NAME =
            "Administrador Inicial";

    private static final String VALID_EMAIL =
            "admin@escala24.local";

    private static final String VALID_PASSWORD =
            "temporary-secure-password";

    @ParameterizedTest
    @MethodSource("invalidConfigurations")
    void shouldRejectInvalidConfiguration(
            InitialAdminProperties properties,
            String expectedMessage
    ) {
        UserRepository userRepository =
                mock(UserRepository.class);

        PasswordEncoder passwordEncoder =
                mock(PasswordEncoder.class);

        when(userRepository.existsByRole(Role.ADMIN))
                .thenReturn(false);

        InitialAdminBootstrap bootstrap =
                new InitialAdminBootstrap(
                        properties,
                        userRepository,
                        passwordEncoder
                );

        assertThatThrownBy(() -> bootstrap.run(
                new DefaultApplicationArguments(
                        new String[0]
                )
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedMessage);

        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }

    @Test
    void shouldRejectEmailUsedByAnotherUser() {
        UserRepository userRepository =
                mock(UserRepository.class);

        PasswordEncoder passwordEncoder =
                mock(PasswordEncoder.class);

        when(userRepository.existsByRole(Role.ADMIN))
                .thenReturn(false);

        when(userRepository.existsByEmail(VALID_EMAIL))
                .thenReturn(true);

        InitialAdminBootstrap bootstrap =
                new InitialAdminBootstrap(
                        validProperties(),
                        userRepository,
                        passwordEncoder
                );

        assertThatThrownBy(() -> bootstrap.run(
                new DefaultApplicationArguments(
                        new String[0]
                )
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "already used by another user"
                );

        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }

    private static Stream<Arguments>
            invalidConfigurations() {
        return Stream.of(
                arguments(
                        properties(
                                null,
                                VALID_EMAIL,
                                VALID_PASSWORD
                        ),
                        "ESCALA24_INITIAL_ADMIN_NAME"
                ),
                arguments(
                        properties(
                                VALID_NAME,
                                " ",
                                VALID_PASSWORD
                        ),
                        "ESCALA24_INITIAL_ADMIN_EMAIL"
                ),
                arguments(
                        properties(
                                VALID_NAME,
                                VALID_EMAIL,
                                " "
                        ),
                        "ESCALA24_INITIAL_ADMIN_PASSWORD"
                ),
                arguments(
                        properties(
                                "A".repeat(151),
                                VALID_EMAIL,
                                VALID_PASSWORD
                        ),
                        "at most 150 characters"
                ),
                arguments(
                        properties(
                                VALID_NAME,
                                "invalid-email",
                                VALID_PASSWORD
                        ),
                        "email is invalid"
                ),
                arguments(
                        properties(
                                VALID_NAME,
                                VALID_EMAIL,
                                "short"
                        ),
                        "between 8 and 72 characters"
                ),
                arguments(
                        properties(
                                VALID_NAME,
                                VALID_EMAIL,
                                "A".repeat(73)
                        ),
                        "between 8 and 72 characters"
                )
        );
    }

    private static InitialAdminProperties
            validProperties() {
        return properties(
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD
        );
    }

    private static InitialAdminProperties properties(
            String name,
            String email,
            String password
    ) {
        return new InitialAdminProperties(
                true,
                name,
                email,
                password
        );
    }
}