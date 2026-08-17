package br.com.escala24.config;

import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;
import br.com.escala24.repository.UserRepository;

@Component
@ConditionalOnProperty(
        prefix = "escala24.bootstrap.admin",
        name = "enabled",
        havingValue = "true"
)
public class InitialAdminBootstrap
        implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    InitialAdminBootstrap.class
            );

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
            );

    private final InitialAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminBootstrap(
            InitialAdminProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            LOGGER.info(
                    "Initial administrator already exists"
            );
            return;
        }

        String name = requireProperty(
                properties.name(),
                "ESCALA24_INITIAL_ADMIN_NAME"
        );

        String email = requireProperty(
                properties.email(),
                "ESCALA24_INITIAL_ADMIN_EMAIL"
        ).toLowerCase(Locale.ROOT);

        String password = requireProperty(
                properties.password(),
                "ESCALA24_INITIAL_ADMIN_PASSWORD"
        );

        validateLength(name, 150, "administrator name");
        validateEmail(email);
        validatePassword(password);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "The initial administrator email "
                            + "is already used by another user"
            );
        }

        User administrator = new User();
        administrator.setName(name);
        administrator.setEmail(email);
        administrator.setPassword(
                passwordEncoder.encode(password)
        );
        administrator.setRole(Role.ADMIN);
        administrator.setActive(true);
        administrator.setMustChangePassword(true);

        userRepository.saveAndFlush(administrator);

        LOGGER.info(
                "Initial administrator created for {}",
                email
        );
    }

    private String requireProperty(
            String value,
            String environmentVariable
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    environmentVariable
                            + " must be configured"
            );
        }

        return value.trim();
    }

    private void validateLength(
            String value,
            int maximum,
            String field
    ) {
        if (value.length() > maximum) {
            throw new IllegalStateException(
                    "The " + field
                            + " must have at most "
                            + maximum
                            + " characters"
            );
        }
    }

    private void validateEmail(String email) {
        validateLength(email, 150, "administrator email");

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalStateException(
                    "The initial administrator email is invalid"
            );
        }
    }

    private void validatePassword(String password) {
        if (password.length() < 8
                || password.length() > 72) {
            throw new IllegalStateException(
                    "The initial administrator password "
                            + "must have between 8 and 72 characters"
            );
        }
    }
}