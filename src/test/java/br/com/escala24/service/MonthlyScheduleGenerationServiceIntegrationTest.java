package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.escala24.IntegrationTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.DutyAssignment;
import br.com.escala24.entity.MonthlySchedule;
import br.com.escala24.repository.MonthlyScheduleRepository;
import br.com.escala24.exception.MonthlyScheduleAlreadyExistsException;
import br.com.escala24.exception.NoEligibleFirefighterException;
import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.dto.MonthlyScheduleGenerationRequest;
import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.DayType;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.Holiday;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.entity.Unavailability;
import br.com.escala24.entity.UnavailabilityStatus;
import br.com.escala24.entity.UnavailabilityType;
import br.com.escala24.entity.User;
import br.com.escala24.repository.DutyAssignmentRepository;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.HolidayRepository;
import br.com.escala24.repository.UnavailabilityRepository;
import br.com.escala24.repository.UserRepository;

@IntegrationTest
@Transactional
class MonthlyScheduleGenerationServiceIntegrationTest {

    @Autowired
    private MonthlyScheduleGenerationService generationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirefighterRepository firefighterRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private UnavailabilityRepository unavailabilityRepository;

    @Autowired
    private DutyAssignmentRepository dutyAssignmentRepository;

    @Autowired
    private MonthlyScheduleRepository monthlyScheduleRepository;

    @Test
    void shouldRejectWhenStaffCannotCoverTheWholeMonth() {
        createFirefighter(50, true);

        assertThatThrownBy(() -> generationService.generate(
                new MonthlyScheduleGenerationRequest(
                        2086,
                        7)))
                .isInstanceOf(
                        NoEligibleFirefighterException.class)
                .hasMessageContaining("2086-07-02");
    }

    @Test
    void shouldIgnorePendingAndRejectedUnavailabilities() {
        Firefighter pendingFirefighter = createFirefighter(60, true);
        Firefighter rejectedFirefighter = createFirefighter(61, true);

        User administrator = userRepository.saveAndFlush(createUser(
                "Administrador das Indisponibilidades",
                "generation-review-admin@escala24.com",
                Role.ADMIN,
                true));

        YearMonth requestedMonth = YearMonth.of(2085, 6);

        createUnavailability(
                pendingFirefighter,
                requestedMonth,
                UnavailabilityStatus.PENDING,
                null);

        createUnavailability(
                rejectedFirefighter,
                requestedMonth,
                UnavailabilityStatus.REJECTED,
                administrator);

        MonthlyScheduleGenerationResponse response = generationService.generate(
                new MonthlyScheduleGenerationRequest(
                        requestedMonth.getYear(),
                        requestedMonth.getMonthValue()));

        assertThat(response.assignments())
                .hasSize(requestedMonth.lengthOfMonth())
                .extracting(DutyAssignmentResponse::firefighterId)
                .contains(
                        pendingFirefighter.getId(),
                        rejectedFirefighter.getId());
    }

    @Test
    void shouldRespectRestAcrossMonthBoundaries() {
        Firefighter previousMonthFirefighter = createFirefighter(30, true);

        Firefighter nextMonthFirefighter = createFirefighter(31, true);

        createFirefighter(32, true);

        YearMonth requestedMonth = YearMonth.of(2087, 5);

        MonthlySchedule previousSchedule = createSchedule(2087, 4);

        createAssignment(
                previousSchedule,
                previousMonthFirefighter,
                requestedMonth.atDay(1).minusDays(1),
                DayType.WEEKDAY);

        MonthlySchedule nextSchedule = createSchedule(2087, 6);

        createAssignment(
                nextSchedule,
                nextMonthFirefighter,
                requestedMonth.atEndOfMonth().plusDays(1),
                DayType.WEEKDAY);

        MonthlyScheduleGenerationResponse response = generationService.generate(
                new MonthlyScheduleGenerationRequest(
                        requestedMonth.getYear(),
                        requestedMonth.getMonthValue()));

        DutyAssignmentResponse firstAssignment = response.assignments().getFirst();

        DutyAssignmentResponse lastAssignment = response.assignments().getLast();

        assertThat(firstAssignment.firefighterId())
                .isNotEqualTo(
                        previousMonthFirefighter.getId());

        assertThat(lastAssignment.firefighterId())
                .isNotEqualTo(
                        nextMonthFirefighter.getId());
    }

    @Test
    void shouldCompensateAnnualSpecialDutyHistory() {
        Firefighter firefighterWithMoreSpecialDuties = createFirefighter(40, true);

        Firefighter firefighterWithoutHistoryOne = createFirefighter(41, true);

        Firefighter firefighterWithoutHistoryTwo = createFirefighter(42, true);

        MonthlySchedule historicalSchedule = createSchedule(2088, 1);

        createAssignment(
                historicalSchedule,
                firefighterWithMoreSpecialDuties,
                LocalDate.of(2088, 1, 1),
                DayType.WEEKEND_OR_HOLIDAY);

        createAssignment(
                historicalSchedule,
                firefighterWithMoreSpecialDuties,
                LocalDate.of(2088, 1, 3),
                DayType.WEEKEND_OR_HOLIDAY);

        createAssignment(
                historicalSchedule,
                firefighterWithMoreSpecialDuties,
                LocalDate.of(2088, 1, 5),
                DayType.WEEKEND_OR_HOLIDAY);

        MonthlyScheduleGenerationResponse response = generationService.generate(
                new MonthlyScheduleGenerationRequest(
                        2088,
                        2));

        Map<Long, Long> generatedSpecialDuties = response.assignments()
                .stream()
                .filter(assignment -> assignment.dayType() == DayType.WEEKEND_OR_HOLIDAY)
                .collect(
                        Collectors.groupingBy(
                                DutyAssignmentResponse::firefighterId,
                                Collectors.counting()));

        long firstAnnualTotal = 3L
                + generatedSpecialDuties.getOrDefault(
                        firefighterWithMoreSpecialDuties.getId(),
                        0L);

        long secondAnnualTotal = generatedSpecialDuties.getOrDefault(
                firefighterWithoutHistoryOne.getId(),
                0L);

        long thirdAnnualTotal = generatedSpecialDuties.getOrDefault(
                firefighterWithoutHistoryTwo.getId(),
                0L);

        long minimumAnnualTotal = Math.min(
                firstAnnualTotal,
                Math.min(secondAnnualTotal, thirdAnnualTotal));

        long maximumAnnualTotal = Math.max(
                firstAnnualTotal,
                Math.max(secondAnnualTotal, thirdAnnualTotal));

        assertThat(maximumAnnualTotal - minimumAnnualTotal)
                .isLessThanOrEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateMonthlySchedule() {
        createFirefighter(10, true);
        createFirefighter(11, true);

        MonthlyScheduleGenerationRequest request = new MonthlyScheduleGenerationRequest(2090, 3);

        generationService.generate(request);

        assertThatThrownBy(() -> generationService.generate(request))
                .isInstanceOf(
                        MonthlyScheduleAlreadyExistsException.class)
                .hasMessageContaining("3")
                .hasMessageContaining("2090");
    }

    @Test
    void shouldRejectGenerationWithoutActiveFirefighters() {
        Firefighter inactiveFirefighter = createFirefighter(20, false);

        MonthlyScheduleGenerationRequest request = new MonthlyScheduleGenerationRequest(2089, 4);

        assertThatThrownBy(() -> generationService.generate(request))
                .isInstanceOf(
                        NoEligibleFirefighterException.class)
                .hasMessageContaining("2089-04-01");

        assertThat(inactiveFirefighter.getUser().isActive())
                .isFalse();
    }

    @Test
    void shouldGenerateCompleteBalancedMonthlySchedule() {
        List<Firefighter> activeFirefighters = List.of(
                createFirefighter(1, true),
                createFirefighter(2, true),
                createFirefighter(3, true),
                createFirefighter(4, true));

        Firefighter inactiveFirefighter = createFirefighter(5, false);

        User administrator = createUser(
                "Administrador da Geração",
                "generation-admin@escala24.com",
                Role.ADMIN,
                true);

        userRepository.saveAndFlush(administrator);

        YearMonth requestedMonth = YearMonth.of(2091, 2);
        LocalDate firstDate = requestedMonth.atDay(1);

        LocalDate weekdayHoliday = firstDate.with(
                TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        LocalDate ordinaryWeekday = weekdayHoliday.plusDays(1);

        LocalDate saturday = firstDate.with(
                TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        Holiday holiday = new Holiday();
        holiday.setDate(weekdayHoliday);
        holiday.setName("Feriado de Teste da Geração");

        holidayRepository.saveAndFlush(holiday);

        Firefighter unavailableFirefighter = activeFirefighters.getFirst();

        Unavailability unavailability = new Unavailability();
        unavailability.setFirefighter(unavailableFirefighter);
        unavailability.setType(
                UnavailabilityType.PERSONAL_COMMITMENT);
        unavailability.setStartDate(weekdayHoliday);
        unavailability.setEndDate(weekdayHoliday);
        unavailability.setReason("Compromisso familiar");
        unavailability.setStatus(UnavailabilityStatus.APPROVED);
        unavailability.setReviewedBy(administrator);
        unavailability.setReviewedAt(LocalDateTime.now());

        unavailabilityRepository.saveAndFlush(unavailability);

        MonthlyScheduleGenerationResponse response = generationService.generate(
                new MonthlyScheduleGenerationRequest(
                        requestedMonth.getYear(),
                        requestedMonth.getMonthValue()));

        assertThat(response.id()).isNotNull();
        assertThat(response.year())
                .isEqualTo(requestedMonth.getYear());
        assertThat(response.month())
                .isEqualTo(requestedMonth.getMonthValue());
        assertThat(response.status())
                .isEqualTo(ScheduleStatus.DRAFT);
        assertThat(response.createdAt()).isNotNull();

        List<DutyAssignmentResponse> assignments = response.assignments();

        assertThat(assignments)
                .hasSize(requestedMonth.lengthOfMonth());

        List<LocalDate> expectedDates = firstDate
                .datesUntil(
                        requestedMonth
                                .atEndOfMonth()
                                .plusDays(1))
                .toList();

        assertThat(assignments)
                .extracting(DutyAssignmentResponse::dutyDate)
                .containsExactlyElementsOf(expectedDates);

        boolean respectsMandatoryRest = IntStream.range(1, assignments.size())
                .allMatch(index -> !assignments.get(index)
                        .firefighterId()
                        .equals(
                                assignments
                                        .get(index - 1)
                                        .firefighterId()));

        assertThat(respectsMandatoryRest).isTrue();

        assertThat(assignments)
                .extracting(
                        DutyAssignmentResponse::firefighterId)
                .doesNotContain(inactiveFirefighter.getId());

        DutyAssignmentResponse holidayAssignment = findAssignment(assignments, weekdayHoliday);

        assertThat(holidayAssignment.dayType())
                .isEqualTo(DayType.WEEKEND_OR_HOLIDAY);

        assertThat(holidayAssignment.firefighterId())
                .isNotEqualTo(unavailableFirefighter.getId());

        assertThat(findAssignment(assignments, saturday).dayType())
                .isEqualTo(DayType.WEEKEND_OR_HOLIDAY);

        assertThat(
                findAssignment(assignments, ordinaryWeekday)
                        .dayType())
                .isEqualTo(DayType.WEEKDAY);

        Map<Long, Long> dutiesByFirefighter = assignments.stream()
                .collect(
                        Collectors.groupingBy(
                                DutyAssignmentResponse::firefighterId,
                                Collectors.counting()));

        long minimumDuties = dutiesByFirefighter.values()
                .stream()
                .mapToLong(Long::longValue)
                .min()
                .orElseThrow();

        long maximumDuties = dutiesByFirefighter.values()
                .stream()
                .mapToLong(Long::longValue)
                .max()
                .orElseThrow();

        assertThat(maximumDuties - minimumDuties)
                .isLessThanOrEqualTo(1);

        assertThat(
                dutyAssignmentRepository
                        .findByMonthlyScheduleIdOrderByDutyDateAsc(
                                response.id()))
                .hasSize(requestedMonth.lengthOfMonth());
    }

    private DutyAssignmentResponse findAssignment(
            List<DutyAssignmentResponse> assignments,
            LocalDate dutyDate) {
        return assignments.stream()
                .filter(assignment -> assignment.dutyDate().equals(dutyDate))
                .findFirst()
                .orElseThrow();
    }

    private MonthlySchedule createSchedule(
            int year,
            int month) {
        MonthlySchedule schedule = new MonthlySchedule();
        schedule.setYear(year);
        schedule.setMonth(month);

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

    private Unavailability createUnavailability(
            Firefighter firefighter,
            YearMonth requestedMonth,
            UnavailabilityStatus status,
            User reviewer) {
        Unavailability unavailability = new Unavailability();
        unavailability.setFirefighter(firefighter);
        unavailability.setType(
                UnavailabilityType.PERSONAL_COMMITMENT);
        unavailability.setStartDate(requestedMonth.atDay(1));
        unavailability.setEndDate(requestedMonth.atEndOfMonth());
        unavailability.setReason("Período que não deve bloquear a geração");
        unavailability.setStatus(status);

        if (reviewer != null) {
            unavailability.setReviewedBy(reviewer);
            unavailability.setReviewedAt(LocalDateTime.now());
        }

        return unavailabilityRepository.saveAndFlush(unavailability);
    }

    private Firefighter createFirefighter(
            int number,
            boolean active) {
        User user = createUser(
                "Bombeiro da Geração " + number,
                "generation-firefighter-"
                        + number
                        + "@escala24.com",
                Role.FIREFIGHTER,
                active);

        userRepository.saveAndFlush(user);

        Firefighter firefighter = new Firefighter();
        firefighter.setUser(user);
        firefighter.setRegistration(
                "REG-GENERATION-" + number);
        firefighter.setPhone("1199999999" + number);

        return firefighterRepository.saveAndFlush(firefighter);
    }

    private User createUser(
            String name,
            String email,
            Role role,
            boolean active) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setActive(active);
        user.setMustChangePassword(true);
        return user;
    }
}
