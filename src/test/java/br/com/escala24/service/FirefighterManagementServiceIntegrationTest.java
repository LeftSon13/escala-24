package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.escala24.IntegrationTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.FirefighterRegistrationRequest;
import br.com.escala24.dto.FirefighterRegistrationResponse;
import br.com.escala24.dto.FirefighterSummaryResponse;
import br.com.escala24.entity.User;
import br.com.escala24.exception.FirefighterNotFoundException;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.UserRepository;

@IntegrationTest
@Transactional
class FirefighterManagementServiceIntegrationTest {

    @Autowired
    private FirefighterManagementService managementService;

    @Autowired
    private FirefighterRegistrationService registrationService;

    @Autowired
    private FirefighterRepository firefighterRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldListActiveAndInactiveFirefighters() {
        FirefighterRegistrationResponse activeFirefighter = registerFirefighter(
                "Bombeiro Ativo",
                "active@escala24.com",
                "REG-300");

        FirefighterRegistrationResponse inactiveFirefighter = registerFirefighter(
                "Bombeiro Inativo",
                "inactive@escala24.com",
                "REG-301");

        managementService.deactivate(
                inactiveFirefighter.firefighterId());

        List<FirefighterSummaryResponse> firefighters = managementService.listAll();

        FirefighterSummaryResponse activeSummary = firefighters
                .stream()
                .filter(summary -> summary.firefighterId()
                        .equals(activeFirefighter.firefighterId()))
                .findFirst()
                .orElseThrow();

        FirefighterSummaryResponse inactiveSummary = firefighters
                .stream()
                .filter(summary -> summary.firefighterId()
                        .equals(inactiveFirefighter.firefighterId()))
                .findFirst()
                .orElseThrow();

        assertThat(activeSummary.active()).isTrue();
        assertThat(inactiveSummary.active()).isFalse();
    }

    @Test
    void shouldDeactivateWithoutDeletingFirefighter() {
        FirefighterRegistrationResponse registeredFirefighter = registerFirefighter(
                "Bombeiro para Desativar",
                "deactivate@escala24.com",
                "REG-302");

        long usersBefore = userRepository.count();
        long firefightersBefore = firefighterRepository.count();

        FirefighterSummaryResponse response = managementService.deactivate(
                registeredFirefighter.firefighterId());

        User savedUser = userRepository
                .findById(registeredFirefighter.userId())
                .orElseThrow();

        assertThat(response.active()).isFalse();
        assertThat(savedUser.isActive()).isFalse();

        assertThat(firefighterRepository.existsById(
                registeredFirefighter.firefighterId())).isTrue();

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(firefighterRepository.count())
                .isEqualTo(firefightersBefore);
    }

    @Test
    void shouldRejectUnknownFirefighterId() {
        long unknownId = Long.MAX_VALUE;

        assertThatThrownBy(() -> managementService.deactivate(unknownId))
                .isInstanceOf(FirefighterNotFoundException.class)
                .hasMessageContaining(String.valueOf(unknownId));
    }

    private FirefighterRegistrationResponse registerFirefighter(
            String name,
            String email,
            String registration) {
        FirefighterRegistrationRequest request = new FirefighterRegistrationRequest(
                name,
                email,
                "temporary-password",
                registration,
                "11999999999");

        return registrationService.register(request);
    }
}
