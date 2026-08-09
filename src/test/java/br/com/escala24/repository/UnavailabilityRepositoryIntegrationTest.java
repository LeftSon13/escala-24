package br.com.escala24.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.Unavailability;
import br.com.escala24.entity.UnavailabilityStatus;
import br.com.escala24.entity.UnavailabilityType;
import br.com.escala24.entity.User;

@SpringBootTest
@Transactional
class UnavailabilityRepositoryIntegrationTest {

    @Autowired
    private UnavailabilityRepository unavailabilityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirefighterRepository firefighterRepository;

    @Test
    void shouldQueryPendingAndApprovedUnavailabilitiesByPeriod() {
        User firefighterUser = createUser(
                "Bombeiro Indisponível",
                "unavailability-repository@escala24.com",
                Role.FIREFIGHTER
        );

        userRepository.saveAndFlush(firefighterUser);

        Firefighter firefighter = new Firefighter();
        firefighter.setUser(firefighterUser);
        firefighter.setRegistration("REG-UNAVAILABLE-REPOSITORY");
        firefighter.setPhone("11999999996");

        firefighterRepository.saveAndFlush(firefighter);

        User administrator = createUser(
                "Administrador da Escala",
                "unavailability-admin@escala24.com",
                Role.ADMIN
        );

        userRepository.saveAndFlush(administrator);

        Unavailability approvedVacation = new Unavailability();
        approvedVacation.setFirefighter(firefighter);
        approvedVacation.setType(UnavailabilityType.VACATION);
        approvedVacation.setStartDate(LocalDate.of(2095, 8, 10));
        approvedVacation.setEndDate(LocalDate.of(2095, 8, 12));
        approvedVacation.setStatus(UnavailabilityStatus.APPROVED);
        approvedVacation.setReviewedBy(administrator);
        approvedVacation.setReviewedAt(
                LocalDateTime.of(2095, 7, 20, 10, 0)
        );

        Unavailability pendingCommitment = new Unavailability();
        pendingCommitment.setFirefighter(firefighter);
        pendingCommitment.setType(
                UnavailabilityType.PERSONAL_COMMITMENT
        );
        pendingCommitment.setStartDate(LocalDate.of(2095, 8, 20));
        pendingCommitment.setEndDate(LocalDate.of(2095, 8, 20));
        pendingCommitment.setReason("Compromisso pessoal");

        unavailabilityRepository.saveAllAndFlush(
                List.of(approvedVacation, pendingCommitment)
        );

        List<Unavailability> pendingRequests =
                unavailabilityRepository
                        .findByStatusOrderByRequestedAtAsc(
                                UnavailabilityStatus.PENDING
                        );

        assertThat(pendingRequests)
                .extracting(Unavailability::getId)
                .contains(pendingCommitment.getId());

        assertThat(pendingRequests)
                .filteredOn(request ->
                        request.getId().equals(
                                pendingCommitment.getId()
                        )
                )
                .first()
                .extracting(request ->
                        request.getFirefighter()
                                .getUser()
                                .getName()
                )
                .isEqualTo("Bombeiro Indisponível");

        List<Unavailability> firefighterHistory =
                unavailabilityRepository
                        .findByFirefighterIdOrderByStartDateDesc(
                                firefighter.getId()
                        );

        assertThat(firefighterHistory)
                .extracting(Unavailability::getStartDate)
                .containsExactly(
                        LocalDate.of(2095, 8, 20),
                        LocalDate.of(2095, 8, 10)
                );

        boolean unavailableDuringVacation =
                unavailabilityRepository
                        .existsByFirefighterIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                firefighter.getId(),
                                UnavailabilityStatus.APPROVED,
                                LocalDate.of(2095, 8, 11),
                                LocalDate.of(2095, 8, 11)
                        );

        boolean unavailableAfterVacation =
                unavailabilityRepository
                        .existsByFirefighterIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                firefighter.getId(),
                                UnavailabilityStatus.APPROVED,
                                LocalDate.of(2095, 8, 13),
                                LocalDate.of(2095, 8, 13)
                        );

        assertThat(unavailableDuringVacation).isTrue();
        assertThat(unavailableAfterVacation).isFalse();

        List<Unavailability> approvedDuringAugust =
                unavailabilityRepository
                        .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                UnavailabilityStatus.APPROVED,
                                LocalDate.of(2095, 8, 31),
                                LocalDate.of(2095, 8, 1)
                        );

        assertThat(approvedDuringAugust)
                .extracting(Unavailability::getId)
                .contains(approvedVacation.getId())
                .doesNotContain(pendingCommitment.getId());
    }

    private User createUser(
            String name,
            String email,
            Role role
    ) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setActive(true);
        user.setMustChangePassword(true);
        return user;
    }
}