package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.escala24.exception.EmailAlreadyExistsException;
import br.com.escala24.exception.RegistrationAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.escala24.IntegrationTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.FirefighterRegistrationRequest;
import br.com.escala24.dto.FirefighterRegistrationResponse;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.UserRepository;

import jakarta.validation.ConstraintViolationException;

@IntegrationTest
@Transactional
class FirefighterRegistrationServiceIntegrationTest {

    @Autowired
    private FirefighterRegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirefighterRepository firefighterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldRegisterFirefighterWithTemporaryPassword() {
        FirefighterRegistrationRequest request = new FirefighterRegistrationRequest(
                "  Maria da Silva  ",
                "MARIA@ESCALA24.COM",
                "temporary-password",
                "  reg-100  ",
                "  11999999999  ");

        FirefighterRegistrationResponse response = registrationService.register(request);

        User savedUser = userRepository
                .findByEmail("maria@escala24.com")
                .orElseThrow();

        Firefighter savedFirefighter = firefighterRepository
                .findByRegistration("REG-100")
                .orElseThrow();

        assertThat(response.firefighterId())
                .isEqualTo(savedFirefighter.getId());
        assertThat(response.userId()).isEqualTo(savedUser.getId());
        assertThat(response.name()).isEqualTo("Maria da Silva");
        assertThat(response.email()).isEqualTo("maria@escala24.com");
        assertThat(response.registration()).isEqualTo("REG-100");
        assertThat(response.phone()).isEqualTo("11999999999");
        assertThat(response.active()).isTrue();
        assertThat(response.mustChangePassword()).isTrue();

        assertThat(savedUser.getRole()).isEqualTo(Role.FIREFIGHTER);
        assertThat(savedUser.isActive()).isTrue();
        assertThat(savedUser.isMustChangePassword()).isTrue();

        assertThat(savedUser.getPassword())
                .isNotEqualTo(request.temporaryPassword());
        assertThat(passwordEncoder.matches(
                request.temporaryPassword(),
                savedUser.getPassword())).isTrue();

        assertThat(savedFirefighter.getUser().getId())
                .isEqualTo(savedUser.getId());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        FirefighterRegistrationRequest firstRequest = new FirefighterRegistrationRequest(
                "Primeiro Bombeiro",
                "duplicate@escala24.com",
                "temporary-password",
                "REG-101",
                "11999999991");

        registrationService.register(firstRequest);

        FirefighterRegistrationRequest duplicateRequest = new FirefighterRegistrationRequest(
                "Segundo Bombeiro",
                "DUPLICATE@ESCALA24.COM",
                "another-password",
                "REG-102",
                "11999999992");

        assertThatThrownBy(() -> registrationService.register(duplicateRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("duplicate@escala24.com");

        assertThat(firefighterRepository.findByRegistration("REG-102"))
                .isEmpty();
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        FirefighterRegistrationRequest firstRequest = new FirefighterRegistrationRequest(
                "Primeiro Bombeiro",
                "first@escala24.com",
                "temporary-password",
                "REG-200",
                "11999999993");

        registrationService.register(firstRequest);

        FirefighterRegistrationRequest duplicateRequest = new FirefighterRegistrationRequest(
                "Segundo Bombeiro",
                "second@escala24.com",
                "another-password",
                "  reg-200  ",
                "11999999994");

        assertThatThrownBy(() -> registrationService.register(duplicateRequest))
                .isInstanceOf(RegistrationAlreadyExistsException.class)
                .hasMessageContaining("REG-200");

        assertThat(userRepository.findByEmail("second@escala24.com"))
                .isEmpty();
    }

    @Test
    void shouldRejectInvalidRequestBeforePersistence() {
        long usersBefore = userRepository.count();
        long firefightersBefore = firefighterRepository.count();

        FirefighterRegistrationRequest invalidRequest = new FirefighterRegistrationRequest(
                "",
                "invalid-email",
                "short",
                "",
                "");

        assertThatThrownBy(() -> registrationService.register(invalidRequest))
                .isInstanceOf(ConstraintViolationException.class);

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(firefighterRepository.count())
                .isEqualTo(firefightersBefore);
    }
}
