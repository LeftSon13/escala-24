package br.com.escala24.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.IntegrationTest;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;
import br.com.escala24.repository.UserRepository;

@IntegrationTest
@TestPropertySource(properties = {
        "escala24.bootstrap.admin.enabled=true",
        "escala24.bootstrap.admin.name=Administrador Inicial",
        "escala24.bootstrap.admin.email=INITIAL-ADMIN@ESCALA24.LOCAL",
        "escala24.bootstrap.admin.password=temporary-secure-password"
})
@Transactional
class InitialAdminBootstrapIntegrationTest {

    @Autowired
    private InitialAdminBootstrap initialAdminBootstrap;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateInitialAdministratorOnlyOnce() {
        User administrator = userRepository
                .findByEmail(
                        "initial-admin@escala24.local"
                )
                .orElseThrow();

        assertThat(administrator.getName())
                .isEqualTo("Administrador Inicial");

        assertThat(administrator.getRole())
                .isEqualTo(Role.ADMIN);

        assertThat(administrator.isActive())
                .isTrue();

        assertThat(administrator.isMustChangePassword())
                .isTrue();

        assertThat(passwordEncoder.matches(
                "temporary-secure-password",
                administrator.getPassword()
        )).isTrue();

        assertThat(userRepository.count()).isEqualTo(1);

        initialAdminBootstrap.run(
                new DefaultApplicationArguments(
                        new String[0]
                )
        );

        assertThat(userRepository.count()).isEqualTo(1);
    }
}