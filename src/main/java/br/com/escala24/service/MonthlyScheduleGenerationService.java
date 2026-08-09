package br.com.escala24.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.dto.MonthlyScheduleGenerationRequest;
import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.DayType;
import br.com.escala24.entity.DutyAssignment;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.MonthlySchedule;
import br.com.escala24.entity.Unavailability;
import br.com.escala24.entity.UnavailabilityStatus;
import br.com.escala24.exception.MonthlyScheduleAlreadyExistsException;
import br.com.escala24.exception.NoEligibleFirefighterException;
import br.com.escala24.repository.DutyAssignmentRepository;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.MonthlyScheduleRepository;
import br.com.escala24.repository.UnavailabilityRepository;
import jakarta.validation.Valid;

@Service
@Validated
public class MonthlyScheduleGenerationService {

    private final MonthlyScheduleRepository monthlyScheduleRepository;
    private final DutyAssignmentRepository dutyAssignmentRepository;
    private final FirefighterRepository firefighterRepository;
    private final UnavailabilityRepository unavailabilityRepository;
    private final DayTypeClassifier dayTypeClassifier;

    public MonthlyScheduleGenerationService(
            MonthlyScheduleRepository monthlyScheduleRepository,
            DutyAssignmentRepository dutyAssignmentRepository,
            FirefighterRepository firefighterRepository,
            UnavailabilityRepository unavailabilityRepository,
            DayTypeClassifier dayTypeClassifier
    ) {
        this.monthlyScheduleRepository = monthlyScheduleRepository;
        this.dutyAssignmentRepository = dutyAssignmentRepository;
        this.firefighterRepository = firefighterRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.dayTypeClassifier = dayTypeClassifier;
    }

    @Transactional
    public MonthlyScheduleGenerationResponse generate(
            @Valid MonthlyScheduleGenerationRequest request
    ) {
        Objects.requireNonNull(
                request,
                "Os dados da geração são obrigatórios"
        );

        YearMonth requestedMonth = YearMonth.of(
                request.year(),
                request.month()
        );

        preventDuplicateSchedule(requestedMonth);

        LocalDate firstDate = requestedMonth.atDay(1);
        LocalDate lastDate = requestedMonth.atEndOfMonth();

        List<Firefighter> activeFirefighters =
                firefighterRepository
                        .findByUserActiveTrueOrderByIdAsc();

        if (activeFirefighters.isEmpty()) {
            throw new NoEligibleFirefighterException(firstDate);
        }

        Map<Long, List<Unavailability>> unavailabilities =
                loadApprovedUnavailabilities(firstDate, lastDate);

        Map<Long, Set<LocalDate>> occupiedDates =
                loadOccupiedDates(firstDate, lastDate);

        Map<Long, Integer> monthlyDutyCounts = new HashMap<>();
        Map<Long, Long> annualSpecialDutyCounts = new HashMap<>();

        initializeCounters(
                activeFirefighters,
                requestedMonth,
                monthlyDutyCounts,
                annualSpecialDutyCounts
        );

        MonthlySchedule schedule = createSchedule(requestedMonth);

        List<DutyAssignment> assignments = new ArrayList<>();

        LocalDate dutyDate = firstDate;

        while (!dutyDate.isAfter(lastDate)) {
            DayType dayType = dayTypeClassifier.classify(dutyDate);

            Firefighter selectedFirefighter = selectFirefighter(
                    activeFirefighters,
                    dutyDate,
                    dayType,
                    unavailabilities,
                    occupiedDates,
                    monthlyDutyCounts,
                    annualSpecialDutyCounts
            );

            DutyAssignment assignment = new DutyAssignment();
            assignment.setMonthlySchedule(schedule);
            assignment.setFirefighter(selectedFirefighter);
            assignment.setDutyDate(dutyDate);
            assignment.setDayType(dayType);

            assignments.add(assignment);

            occupiedDates
                    .computeIfAbsent(
                            selectedFirefighter.getId(),
                            ignored -> new HashSet<>()
                    )
                    .add(dutyDate);

            monthlyDutyCounts.merge(
                    selectedFirefighter.getId(),
                    1,
                    Integer::sum
            );

            if (dayType == DayType.WEEKEND_OR_HOLIDAY) {
                annualSpecialDutyCounts.merge(
                        selectedFirefighter.getId(),
                        1L,
                        Long::sum
                );
            }

            dutyDate = dutyDate.plusDays(1);
        }

        List<DutyAssignment> savedAssignments =
                dutyAssignmentRepository
                        .saveAllAndFlush(assignments);

        return toResponse(schedule, savedAssignments);
    }

    private void preventDuplicateSchedule(YearMonth requestedMonth) {
        boolean scheduleExists =
                monthlyScheduleRepository.existsByYearAndMonth(
                        requestedMonth.getYear(),
                        requestedMonth.getMonthValue()
                );

        if (scheduleExists) {
            throw new MonthlyScheduleAlreadyExistsException(
                    requestedMonth.getYear(),
                    requestedMonth.getMonthValue()
            );
        }
    }

    private Map<Long, List<Unavailability>>
            loadApprovedUnavailabilities(
                    LocalDate firstDate,
                    LocalDate lastDate
            ) {
        List<Unavailability> approvedUnavailabilities =
                unavailabilityRepository
                        .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                UnavailabilityStatus.APPROVED,
                                lastDate,
                                firstDate
                        );

        Map<Long, List<Unavailability>> result = new HashMap<>();

        for (Unavailability unavailability
                : approvedUnavailabilities) {
            Long firefighterId =
                    unavailability.getFirefighter().getId();

            result.computeIfAbsent(
                    firefighterId,
                    ignored -> new ArrayList<>()
            ).add(unavailability);
        }

        return result;
    }

    private Map<Long, Set<LocalDate>> loadOccupiedDates(
            LocalDate firstDate,
            LocalDate lastDate
    ) {
        List<DutyAssignment> existingAssignments =
                dutyAssignmentRepository.findByDutyDateBetween(
                        firstDate.minusDays(1),
                        lastDate.plusDays(1)
                );

        Map<Long, Set<LocalDate>> result = new HashMap<>();

        for (DutyAssignment assignment : existingAssignments) {
            Long firefighterId =
                    assignment.getFirefighter().getId();

            result.computeIfAbsent(
                    firefighterId,
                    ignored -> new HashSet<>()
            ).add(assignment.getDutyDate());
        }

        return result;
    }

    private void initializeCounters(
            List<Firefighter> firefighters,
            YearMonth requestedMonth,
            Map<Long, Integer> monthlyDutyCounts,
            Map<Long, Long> annualSpecialDutyCounts
    ) {
        LocalDate firstDayOfYear =
                LocalDate.of(requestedMonth.getYear(), 1, 1);

        LocalDate lastDayOfYear =
                LocalDate.of(requestedMonth.getYear(), 12, 31);

        for (Firefighter firefighter : firefighters) {
            Long firefighterId = firefighter.getId();

            monthlyDutyCounts.put(firefighterId, 0);

            long annualSpecialDuties =
                    dutyAssignmentRepository
                            .countByFirefighterIdAndDayTypeAndDutyDateBetween(
                                    firefighterId,
                                    DayType.WEEKEND_OR_HOLIDAY,
                                    firstDayOfYear,
                                    lastDayOfYear
                            );

            annualSpecialDutyCounts.put(
                    firefighterId,
                    annualSpecialDuties
            );
        }
    }

    private MonthlySchedule createSchedule(
            YearMonth requestedMonth
    ) {
        MonthlySchedule schedule = new MonthlySchedule();
        schedule.setYear(requestedMonth.getYear());
        schedule.setMonth(requestedMonth.getMonthValue());

        return monthlyScheduleRepository.saveAndFlush(schedule);
    }

    private Firefighter selectFirefighter(
            List<Firefighter> firefighters,
            LocalDate dutyDate,
            DayType dayType,
            Map<Long, List<Unavailability>> unavailabilities,
            Map<Long, Set<LocalDate>> occupiedDates,
            Map<Long, Integer> monthlyDutyCounts,
            Map<Long, Long> annualSpecialDutyCounts
    ) {
        return firefighters.stream()
                .filter(firefighter ->
                        isAvailable(
                                firefighter,
                                dutyDate,
                                unavailabilities,
                                occupiedDates
                        )
                )
                .min(
                        candidateComparator(
                                dutyDate,
                                dayType,
                                occupiedDates,
                                monthlyDutyCounts,
                                annualSpecialDutyCounts
                        )
                )
                .orElseThrow(() ->
                        new NoEligibleFirefighterException(dutyDate)
                );
    }

    private boolean isAvailable(
            Firefighter firefighter,
            LocalDate dutyDate,
            Map<Long, List<Unavailability>> unavailabilities,
            Map<Long, Set<LocalDate>> occupiedDates
    ) {
        Long firefighterId = firefighter.getId();

        boolean unavailable =
                unavailabilities
                        .getOrDefault(
                                firefighterId,
                                List.of()
                        )
                        .stream()
                        .anyMatch(item ->
                                !dutyDate.isBefore(item.getStartDate())
                                        && !dutyDate.isAfter(
                                                item.getEndDate()
                                        )
                        );

        if (unavailable) {
            return false;
        }

        Set<LocalDate> firefighterDates =
                occupiedDates.getOrDefault(
                        firefighterId,
                        Set.of()
                );

        return !firefighterDates.contains(dutyDate.minusDays(1))
                && !firefighterDates.contains(dutyDate)
                && !firefighterDates.contains(dutyDate.plusDays(1));
    }

    private Comparator<Firefighter> candidateComparator(
            LocalDate dutyDate,
            DayType dayType,
            Map<Long, Set<LocalDate>> occupiedDates,
            Map<Long, Integer> monthlyDutyCounts,
            Map<Long, Long> annualSpecialDutyCounts
    ) {
        Comparator<Firefighter> monthlyBalance =
                Comparator.comparingInt(firefighter ->
                        monthlyDutyCounts.get(firefighter.getId())
                );

        Comparator<Firefighter> specialBalance =
                Comparator.comparingLong(firefighter ->
                        annualSpecialDutyCounts.get(
                                firefighter.getId()
                        )
                );

        Comparator<Firefighter> oldestPreviousDuty =
                Comparator.comparing(
                        firefighter -> findPreviousDuty(
                                firefighter.getId(),
                                dutyDate,
                                occupiedDates
                        ),
                        Comparator.nullsFirst(
                                Comparator.naturalOrder()
                        )
                );

        Comparator<Firefighter> comparator;

        if (dayType == DayType.WEEKEND_OR_HOLIDAY) {
            comparator = specialBalance
                    .thenComparing(monthlyBalance);
        } else {
            comparator = monthlyBalance
                    .thenComparing(specialBalance.reversed());
        }

        return comparator
                .thenComparing(oldestPreviousDuty)
                .thenComparing(Firefighter::getId);
    }

    private LocalDate findPreviousDuty(
            Long firefighterId,
            LocalDate dutyDate,
            Map<Long, Set<LocalDate>> occupiedDates
    ) {
        return occupiedDates
                .getOrDefault(firefighterId, Set.of())
                .stream()
                .filter(date -> date.isBefore(dutyDate))
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private MonthlyScheduleGenerationResponse toResponse(
            MonthlySchedule schedule,
            List<DutyAssignment> assignments
    ) {
        List<DutyAssignmentResponse> assignmentResponses =
                assignments.stream()
                        .sorted(
                                Comparator.comparing(
                                        DutyAssignment::getDutyDate
                                )
                        )
                        .map(this::toAssignmentResponse)
                        .toList();

        return new MonthlyScheduleGenerationResponse(
                schedule.getId(),
                schedule.getYear(),
                schedule.getMonth(),
                schedule.getStatus(),
                schedule.getCreatedAt(),
                assignmentResponses
        );
    }

    private DutyAssignmentResponse toAssignmentResponse(
            DutyAssignment assignment
    ) {
        Firefighter firefighter = assignment.getFirefighter();

        return new DutyAssignmentResponse(
                assignment.getId(),
                assignment.getDutyDate(),
                assignment.getStartDateTime(),
                assignment.getEndDateTime(),
                assignment.getDayType(),
                firefighter.getId(),
                firefighter.getUser().getName(),
                firefighter.getRegistration()
        );
    }
}