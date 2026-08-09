package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.UnavailabilityRequest;
import br.com.escala24.dto.UnavailabilityResponse;
import br.com.escala24.dto.UnavailabilityReviewResponse;
import br.com.escala24.entity.DayType;
import br.com.escala24.entity.DutyAssignment;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.MonthlySchedule;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.Unavailability;
import br.com.escala24.entity.UnavailabilityStatus;
import br.com.escala24.entity.UnavailabilityType;
import br.com.escala24.entity.User;
import br.com.escala24.exception.AdministratorRequiredException;
import br.com.escala24.exception.DutyReassignmentRequiredException;
import br.com.escala24.exception.InvalidUnavailabilityPeriodException;
import br.com.escala24.exception.UnavailabilityAlreadyReviewedException;
import br.com.escala24.repository.DutyAssignmentRepository;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.MonthlyScheduleRepository;
import br.com.escala24.repository.UnavailabilityRepository;
import br.com.escala24.repository.UserRepository;

@SpringBootTest
@Transactional
class UnavailabilityManagementServiceIntegrationTest {

    @Autowired
    private UnavailabilityManagementService managementService;

    @Autowired
    private UnavailabilityRepository unavailabilityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirefighterRepository firefighterRepository;

    @Autowired
    private MonthlyScheduleRepository monthlyScheduleRepository;

    @Autowired
    private DutyAssignmentRepository dutyAssignmentRepository;

    private Long firefighterId;
    private Long administratorId;
    private Long regularUserId;

    @BeforeEach
    void setUp() {
        User firefighterUser = createUser(
                "Bombeiro Solicitante",
                "requester@escala24.com",
                Role.FIREFIGHTER);

        User administrator = createUser(
                "Administrador Avaliador",
                "reviewer-admin@escala24.com",
                Role.ADMIN);

        User regularUser = createUser(
                "Bombeiro sem Permissão",
                "non-admin-reviewer@escala24.com",
                Role.FIREFIGHTER);

        userRepository.saveAllAndFlush(
                List.of(
                        firefighterUser,
                        administrator,
                        regularUser));

        Firefighter firefighter = new Firefighter();
        firefighter.setUser(firefighterUser);
        firefighter.setRegistration("REG-REQUESTER-001");
        firefighter.setPhone("11999999995");

        firefighterRepository.saveAndFlush(firefighter);

        firefighterId = firefighter.getId();
        administratorId = administrator.getId();
        regularUserId = regularUser.getId();
    }

    @Test
    void shouldRequestListAndApproveUnavailability() {
        UnavailabilityRequest request = new UnavailabilityRequest(
                UnavailabilityType.PERSONAL_COMMITMENT,
                LocalDate.of(2094, 8, 20),
                LocalDate.of(2094, 8, 20),
                "  Compromisso familiar  ");

        UnavailabilityResponse requested = managementService.request(
                firefighterId,
                request);

        assertThat(requested.id()).isNotNull();
        assertThat(requested.firefighterId())
                .isEqualTo(firefighterId);
        assertThat(requested.status())
                .isEqualTo(UnavailabilityStatus.PENDING);
        assertThat(requested.reviewedAt()).isNull();

        List<UnavailabilityReviewResponse> pendingRequests = managementService.listPending();

        UnavailabilityReviewResponse pendingRequest = pendingRequests.stream()
                .filter(item -> item.id().equals(requested.id()))
                .findFirst()
                .orElseThrow();

        assertThat(pendingRequest.reason())
                .isEqualTo("Compromisso familiar");

        UnavailabilityResponse approved = managementService.approve(
                requested.id(),
                administratorId);

        unavailabilityRepository.flush();

        assertThat(approved.status())
                .isEqualTo(UnavailabilityStatus.APPROVED);
        assertThat(approved.reviewedAt()).isNotNull();

        Unavailability savedUnavailability = unavailabilityRepository
                .findById(requested.id())
                .orElseThrow();

        assertThat(savedUnavailability.getReviewedBy().getId())
                .isEqualTo(administratorId);
        assertThat(savedUnavailability.getReviewedAt())
                .isNotNull();
    }

    @Test
    void shouldRejectAndPreventSecondReview() {
        UnavailabilityResponse requested = managementService.request(
                firefighterId,
                new UnavailabilityRequest(
                        UnavailabilityType.OTHER,
                        LocalDate.of(2094, 9, 5),
                        LocalDate.of(2094, 9, 5),
                        null));

        UnavailabilityResponse rejected = managementService.reject(
                requested.id(),
                administratorId);

        unavailabilityRepository.flush();

        assertThat(rejected.status())
                .isEqualTo(UnavailabilityStatus.REJECTED);

        assertThatThrownBy(() -> managementService.approve(
                requested.id(),
                administratorId))
                .isInstanceOf(
                        UnavailabilityAlreadyReviewedException.class);
    }

    @Test
    void shouldRejectInvalidPeriodAndNonAdministrator() {
        UnavailabilityRequest invalidRequest = new UnavailabilityRequest(
                UnavailabilityType.VACATION,
                LocalDate.of(2094, 10, 10),
                LocalDate.of(2094, 10, 5),
                null);

        assertThatThrownBy(() -> managementService.request(
                firefighterId,
                invalidRequest))
                .isInstanceOf(
                        InvalidUnavailabilityPeriodException.class);

        UnavailabilityResponse requested = managementService.request(
                firefighterId,
                new UnavailabilityRequest(
                        UnavailabilityType.PERSONAL_COMMITMENT,
                        LocalDate.of(2094, 10, 15),
                        LocalDate.of(2094, 10, 15),
                        null));

        assertThatThrownBy(() -> managementService.approve(
                requested.id(),
                regularUserId))
                .isInstanceOf(
                        AdministratorRequiredException.class);
    }

    @Test
    void shouldRequireReassignmentBeforeApproval() {
        Firefighter firefighter = firefighterRepository
                .findById(firefighterId)
                .orElseThrow();

        MonthlySchedule schedule = new MonthlySchedule();
        schedule.setYear(2094);
        schedule.setMonth(11);

        monthlyScheduleRepository.saveAndFlush(schedule);

        DutyAssignment assignment = new DutyAssignment();
        assignment.setMonthlySchedule(schedule);
        assignment.setFirefighter(firefighter);
        assignment.setDutyDate(LocalDate.of(2094, 11, 10));
        assignment.setDayType(DayType.WEEKDAY);

        dutyAssignmentRepository.saveAndFlush(assignment);

        UnavailabilityResponse requested = managementService.request(
                firefighterId,
                new UnavailabilityRequest(
                        UnavailabilityType.PERSONAL_COMMITMENT,
                        LocalDate.of(2094, 11, 10),
                        LocalDate.of(2094, 11, 10),
                        "Compromisso urgente"));

        assertThatThrownBy(() -> managementService.approve(
                requested.id(),
                administratorId))
                .isInstanceOf(
                        DutyReassignmentRequiredException.class);

        Unavailability savedUnavailability = unavailabilityRepository
                .findById(requested.id())
                .orElseThrow();

        assertThat(savedUnavailability.getStatus())
                .isEqualTo(UnavailabilityStatus.PENDING);
        assertThat(savedUnavailability.getReviewedBy()).isNull();
        assertThat(savedUnavailability.getReviewedAt()).isNull();
    }

    private User createUser(
            String name,
            String email,
            Role role) {
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