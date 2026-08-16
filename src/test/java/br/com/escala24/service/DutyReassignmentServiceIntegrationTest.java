package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.escala24.IntegrationTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.dto.DutyReassignmentRequest;
import br.com.escala24.entity.DayType;
import br.com.escala24.entity.DutyAssignment;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.MonthlySchedule;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.entity.Unavailability;
import br.com.escala24.entity.UnavailabilityStatus;
import br.com.escala24.entity.UnavailabilityType;
import br.com.escala24.entity.User;
import br.com.escala24.exception.DutyAssignmentNotFoundException;
import br.com.escala24.exception.FirefighterUnavailableForDutyException;
import br.com.escala24.exception.InactiveFirefighterException;
import br.com.escala24.exception.MandatoryRestViolationException;
import br.com.escala24.exception.PublishedScheduleModificationException;
import br.com.escala24.repository.DutyAssignmentRepository;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.MonthlyScheduleRepository;
import br.com.escala24.repository.UnavailabilityRepository;
import br.com.escala24.repository.UserRepository;

@IntegrationTest
@Transactional
class DutyReassignmentServiceIntegrationTest {

    @Autowired
    private DutyReassignmentService reassignmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirefighterRepository firefighterRepository;

    @Autowired
    private MonthlyScheduleRepository monthlyScheduleRepository;

    @Autowired
    private DutyAssignmentRepository dutyAssignmentRepository;

    @Autowired
    private UnavailabilityRepository unavailabilityRepository;

    @Test
    void shouldRejectReplacementWithApprovedUnavailability() {
        Firefighter originalFirefighter = createFirefighter(110, true);

        Firefighter unavailableReplacement = createFirefighter(111, true);

        User administrator = new User();
        administrator.setName("Administrador da Troca");
        administrator.setEmail(
                "duty-reassignment-admin@escala24.com");
        administrator.setPassword("encoded-password");
        administrator.setRole(Role.ADMIN);
        administrator.setActive(true);
        administrator.setMustChangePassword(true);

        userRepository.saveAndFlush(administrator);

        MonthlySchedule schedule = createSchedule(2076, 10, ScheduleStatus.DRAFT);

        LocalDate dutyDate = LocalDate.of(2076, 10, 14);

        DutyAssignment assignment = createAssignment(
                schedule,
                originalFirefighter,
                dutyDate,
                DayType.WEEKDAY);

        Unavailability unavailability = new Unavailability();
        unavailability.setFirefighter(unavailableReplacement);
        unavailability.setType(
                UnavailabilityType.PERSONAL_COMMITMENT);
        unavailability.setStartDate(dutyDate);
        unavailability.setEndDate(dutyDate);
        unavailability.setReason("Compromisso aprovado");
        unavailability.setStatus(UnavailabilityStatus.APPROVED);
        unavailability.setReviewedBy(administrator);
        unavailability.setReviewedAt(LocalDateTime.now());

        unavailabilityRepository.saveAndFlush(unavailability);

        assertThatThrownBy(() -> reassignmentService.reassign(
                2076,
                10,
                dutyDate,
                new DutyReassignmentRequest(
                        unavailableReplacement.getId())))
                .isInstanceOf(
                        FirefighterUnavailableForDutyException.class);

        dutyAssignmentRepository.flush();

        DutyAssignment unchangedAssignment = dutyAssignmentRepository
                .findById(assignment.getId())
                .orElseThrow();

        assertThat(unchangedAssignment.getFirefighter().getId())
                .isEqualTo(originalFirefighter.getId());
    }

    @Test
    void shouldRespectMandatoryRestAcrossMonthBoundary() {
        Firefighter originalFirefighter = createFirefighter(112, true);

        Firefighter replacementFirefighter = createFirefighter(113, true);

        MonthlySchedule previousSchedule = createSchedule(2075, 10, ScheduleStatus.DRAFT);

        createAssignment(
                previousSchedule,
                replacementFirefighter,
                LocalDate.of(2075, 10, 31),
                DayType.WEEKDAY);

        MonthlySchedule requestedSchedule = createSchedule(2075, 11, ScheduleStatus.DRAFT);

        LocalDate dutyDate = LocalDate.of(2075, 11, 1);

        DutyAssignment assignment = createAssignment(
                requestedSchedule,
                originalFirefighter,
                dutyDate,
                DayType.WEEKEND_OR_HOLIDAY);

        assertThatThrownBy(() -> reassignmentService.reassign(
                2075,
                11,
                dutyDate,
                new DutyReassignmentRequest(
                        replacementFirefighter.getId())))
                .isInstanceOf(
                        MandatoryRestViolationException.class);

        dutyAssignmentRepository.flush();

        DutyAssignment unchangedAssignment = dutyAssignmentRepository
                .findById(assignment.getId())
                .orElseThrow();

        assertThat(unchangedAssignment.getFirefighter().getId())
                .isEqualTo(originalFirefighter.getId());
    }

    @Test
    void shouldRejectInactiveReplacement() {
        Firefighter originalFirefighter = createFirefighter(107, true);

        Firefighter inactiveReplacement = createFirefighter(108, false);

        MonthlySchedule schedule = createSchedule(2078, 8, ScheduleStatus.DRAFT);

        LocalDate dutyDate = LocalDate.of(2078, 8, 20);

        DutyAssignment assignment = createAssignment(
                schedule,
                originalFirefighter,
                dutyDate,
                DayType.WEEKDAY);

        assertThatThrownBy(() -> reassignmentService.reassign(
                2078,
                8,
                dutyDate,
                new DutyReassignmentRequest(
                        inactiveReplacement.getId())))
                .isInstanceOf(
                        InactiveFirefighterException.class);

        dutyAssignmentRepository.flush();

        DutyAssignment unchangedAssignment = dutyAssignmentRepository
                .findById(assignment.getId())
                .orElseThrow();

        assertThat(unchangedAssignment.getFirefighter().getId())
                .isEqualTo(originalFirefighter.getId());
    }

    @Test
    void shouldRejectWhenDutyAssignmentDoesNotExist() {
        Firefighter replacementFirefighter = createFirefighter(109, true);

        createSchedule(2077, 9, ScheduleStatus.DRAFT);

        LocalDate missingDate = LocalDate.of(2077, 9, 18);

        assertThatThrownBy(() -> reassignmentService.reassign(
                2077,
                9,
                missingDate,
                new DutyReassignmentRequest(
                        replacementFirefighter.getId())))
                .isInstanceOf(
                        DutyAssignmentNotFoundException.class)
                .hasMessageContaining(
                        missingDate.toString());
    }

    @Test
    void shouldReassignDutyAndPreserveAssignmentData() {
        Firefighter originalFirefighter = createFirefighter(101, true);

        Firefighter replacementFirefighter = createFirefighter(102, true);

        MonthlySchedule schedule = createSchedule(2081, 5, ScheduleStatus.DRAFT);

        LocalDate dutyDate = LocalDate.of(2081, 5, 15);

        DutyAssignment originalAssignment = createAssignment(
                schedule,
                originalFirefighter,
                dutyDate,
                DayType.WEEKDAY);

        DutyAssignmentResponse response = reassignmentService.reassign(
                2081,
                5,
                dutyDate,
                new DutyReassignmentRequest(
                        replacementFirefighter.getId()));

        dutyAssignmentRepository.flush();

        assertThat(response.id())
                .isEqualTo(originalAssignment.getId());
        assertThat(response.dutyDate()).isEqualTo(dutyDate);
        assertThat(response.dayType())
                .isEqualTo(DayType.WEEKDAY);
        assertThat(response.firefighterId())
                .isEqualTo(replacementFirefighter.getId());

        DutyAssignment savedAssignment = dutyAssignmentRepository
                .findById(originalAssignment.getId())
                .orElseThrow();

        assertThat(savedAssignment.getFirefighter().getId())
                .isEqualTo(replacementFirefighter.getId());
        assertThat(savedAssignment.getMonthlySchedule().getId())
                .isEqualTo(schedule.getId());
        assertThat(savedAssignment.getDutyDate())
                .isEqualTo(dutyDate);
        assertThat(savedAssignment.getDayType())
                .isEqualTo(DayType.WEEKDAY);

        assertThat(
                dutyAssignmentRepository
                        .findByMonthlyScheduleIdOrderByDutyDateAsc(
                                schedule.getId()))
                .hasSize(1);
    }

    @Test
    void shouldRejectModificationOfPublishedSchedule() {
        Firefighter originalFirefighter = createFirefighter(103, true);

        Firefighter replacementFirefighter = createFirefighter(104, true);

        MonthlySchedule schedule = createSchedule(
                2080,
                6,
                ScheduleStatus.PUBLISHED);

        LocalDate dutyDate = LocalDate.of(2080, 6, 10);

        DutyAssignment assignment = createAssignment(
                schedule,
                originalFirefighter,
                dutyDate,
                DayType.WEEKDAY);

        assertThatThrownBy(() -> reassignmentService.reassign(
                2080,
                6,
                dutyDate,
                new DutyReassignmentRequest(
                        replacementFirefighter.getId())))
                .isInstanceOf(
                        PublishedScheduleModificationException.class);

        dutyAssignmentRepository.flush();

        DutyAssignment unchangedAssignment = dutyAssignmentRepository
                .findById(assignment.getId())
                .orElseThrow();

        assertThat(unchangedAssignment.getFirefighter().getId())
                .isEqualTo(originalFirefighter.getId());
    }

    @Test
    void shouldRejectReplacementWithoutMandatoryRest() {
        Firefighter originalFirefighter = createFirefighter(105, true);

        Firefighter replacementFirefighter = createFirefighter(106, true);

        MonthlySchedule schedule = createSchedule(2079, 7, ScheduleStatus.DRAFT);

        LocalDate dutyDate = LocalDate.of(2079, 7, 12);

        DutyAssignment assignment = createAssignment(
                schedule,
                originalFirefighter,
                dutyDate,
                DayType.WEEKDAY);

        createAssignment(
                schedule,
                replacementFirefighter,
                dutyDate.minusDays(1),
                DayType.WEEKDAY);

        assertThatThrownBy(() -> reassignmentService.reassign(
                2079,
                7,
                dutyDate,
                new DutyReassignmentRequest(
                        replacementFirefighter.getId())))
                .isInstanceOf(
                        MandatoryRestViolationException.class);

        dutyAssignmentRepository.flush();

        DutyAssignment unchangedAssignment = dutyAssignmentRepository
                .findById(assignment.getId())
                .orElseThrow();

        assertThat(unchangedAssignment.getFirefighter().getId())
                .isEqualTo(originalFirefighter.getId());
    }

    private MonthlySchedule createSchedule(
            int year,
            int month,
            ScheduleStatus status) {
        MonthlySchedule schedule = new MonthlySchedule();
        schedule.setYear(year);
        schedule.setMonth(month);
        schedule.setStatus(status);

        return monthlyScheduleRepository.saveAndFlush(schedule);
    }

    private DutyAssignment createAssignment(
            MonthlySchedule schedule,
            Firefighter firefighter,
            LocalDate dutyDate,
            DayType dayType) {
        DutyAssignment assignment = new DutyAssignment();
        assignment.setMonthlySchedule(schedule);
        assignment.setFirefighter(firefighter);
        assignment.setDutyDate(dutyDate);
        assignment.setDayType(dayType);

        return dutyAssignmentRepository.saveAndFlush(assignment);
    }

    private Firefighter createFirefighter(
            int number,
            boolean active) {
        User user = new User();
        user.setName("Bombeiro da Troca " + number);
        user.setEmail(
                "duty-reassignment-"
                        + number
                        + "@escala24.com");
        user.setPassword("encoded-password");
        user.setRole(Role.FIREFIGHTER);
        user.setActive(active);
        user.setMustChangePassword(true);

        userRepository.saveAndFlush(user);

        Firefighter firefighter = new Firefighter();
        firefighter.setUser(user);
        firefighter.setRegistration(
                "REG-REASSIGNMENT-" + number);
        firefighter.setPhone("11888888" + number);

        return firefighterRepository.saveAndFlush(firefighter);
    }
}
