package br.com.escala24.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import br.com.escala24.IntegrationTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@IntegrationTest
@Transactional
class UnavailabilityPersistenceIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldPersistOneDayPersonalCommitmentAsPending() {
        User user = new User();
        user.setName("Bombeiro com compromisso");
        user.setEmail("unavailability-model@escala24.com");
        user.setPassword("encoded-password");
        user.setRole(Role.FIREFIGHTER);
        user.setActive(true);
        user.setMustChangePassword(true);

        entityManager.persist(user);

        Firefighter firefighter = new Firefighter();
        firefighter.setUser(user);
        firefighter.setRegistration("REG-UNAVAILABLE-001");
        firefighter.setPhone("11999999997");

        entityManager.persist(firefighter);

        Unavailability unavailability = new Unavailability();
        unavailability.setFirefighter(firefighter);
        unavailability.setType(
                UnavailabilityType.PERSONAL_COMMITMENT
        );
        unavailability.setStartDate(LocalDate.of(2096, 8, 15));
        unavailability.setEndDate(LocalDate.of(2096, 8, 15));
        unavailability.setReason(
                "Compromisso familiar durante o dia"
        );

        entityManager.persist(unavailability);
        entityManager.flush();

        Long unavailabilityId = unavailability.getId();

        entityManager.clear();

        Unavailability savedUnavailability =
                entityManager.find(
                        Unavailability.class,
                        unavailabilityId
                );

        assertThat(savedUnavailability).isNotNull();
        assertThat(savedUnavailability.getType())
                .isEqualTo(
                        UnavailabilityType.PERSONAL_COMMITMENT
                );
        assertThat(savedUnavailability.getStartDate())
                .isEqualTo(LocalDate.of(2096, 8, 15));
        assertThat(savedUnavailability.getEndDate())
                .isEqualTo(LocalDate.of(2096, 8, 15));
        assertThat(savedUnavailability.getReason())
                .isEqualTo(
                        "Compromisso familiar durante o dia"
                );
        assertThat(savedUnavailability.getStatus())
                .isEqualTo(UnavailabilityStatus.PENDING);
        assertThat(savedUnavailability.getRequestedAt())
                .isNotNull();
        assertThat(savedUnavailability.getReviewedBy())
                .isNull();
        assertThat(savedUnavailability.getReviewedAt())
                .isNull();

        assertThat(savedUnavailability
                .getFirefighter()
                .getRegistration())
                .isEqualTo("REG-UNAVAILABLE-001");
    }
}
