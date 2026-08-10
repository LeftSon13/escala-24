package br.com.escala24.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.dto.DutyReassignmentRequest;
import br.com.escala24.entity.DutyAssignment;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.MonthlySchedule;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.entity.UnavailabilityStatus;
import br.com.escala24.exception.DutyAssignmentNotFoundException;
import br.com.escala24.exception.FirefighterNotFoundException;
import br.com.escala24.exception.FirefighterUnavailableForDutyException;
import br.com.escala24.exception.InactiveFirefighterException;
import br.com.escala24.exception.MandatoryRestViolationException;
import br.com.escala24.exception.MonthlyScheduleNotFoundException;
import br.com.escala24.exception.PublishedScheduleModificationException;
import br.com.escala24.repository.DutyAssignmentRepository;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.MonthlyScheduleRepository;
import br.com.escala24.repository.UnavailabilityRepository;

@Service
public class DutyReassignmentService {

    private final MonthlyScheduleRepository monthlyScheduleRepository;
    private final DutyAssignmentRepository dutyAssignmentRepository;
    private final FirefighterRepository firefighterRepository;
    private final UnavailabilityRepository unavailabilityRepository;

    public DutyReassignmentService(
            MonthlyScheduleRepository monthlyScheduleRepository,
            DutyAssignmentRepository dutyAssignmentRepository,
            FirefighterRepository firefighterRepository,
            UnavailabilityRepository unavailabilityRepository
    ) {
        this.monthlyScheduleRepository = monthlyScheduleRepository;
        this.dutyAssignmentRepository = dutyAssignmentRepository;
        this.firefighterRepository = firefighterRepository;
        this.unavailabilityRepository = unavailabilityRepository;
    }

    @Transactional
    public DutyAssignmentResponse reassign(
            int year,
            int month,
            LocalDate dutyDate,
            DutyReassignmentRequest request
    ) {
        YearMonth requestedMonth = YearMonth.of(year, month);

        MonthlySchedule schedule = findSchedule(requestedMonth);

        validateDraftSchedule(schedule);

        DutyAssignment assignment = findAssignment(
                schedule,
                requestedMonth,
                dutyDate
        );

        Firefighter replacement = findFirefighter(
                request.firefighterId()
        );

        validateActiveFirefighter(replacement);
        validateAvailability(replacement, dutyDate);
        validateMandatoryRest(
                replacement,
                assignment,
                dutyDate
        );

        assignment.setFirefighter(replacement);

        DutyAssignment savedAssignment =
                dutyAssignmentRepository.saveAndFlush(assignment);

        return toResponse(savedAssignment);
    }

    private MonthlySchedule findSchedule(
            YearMonth requestedMonth
    ) {
        return monthlyScheduleRepository
                .findByYearAndMonth(
                        requestedMonth.getYear(),
                        requestedMonth.getMonthValue()
                )
                .orElseThrow(() ->
                        new MonthlyScheduleNotFoundException(
                                requestedMonth.getYear(),
                                requestedMonth.getMonthValue()
                        )
                );
    }

    private void validateDraftSchedule(
            MonthlySchedule schedule
    ) {
        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            throw new PublishedScheduleModificationException(
                    schedule.getYear(),
                    schedule.getMonth()
            );
        }
    }

    private DutyAssignment findAssignment(
            MonthlySchedule schedule,
            YearMonth requestedMonth,
            LocalDate dutyDate
    ) {
        if (!YearMonth.from(dutyDate).equals(requestedMonth)) {
            throw new DutyAssignmentNotFoundException(
                    requestedMonth.getYear(),
                    requestedMonth.getMonthValue(),
                    dutyDate
            );
        }

        return dutyAssignmentRepository
                .findByMonthlyScheduleIdAndDutyDate(
                        schedule.getId(),
                        dutyDate
                )
                .orElseThrow(() ->
                        new DutyAssignmentNotFoundException(
                                requestedMonth.getYear(),
                                requestedMonth.getMonthValue(),
                                dutyDate
                        )
                );
    }

    private Firefighter findFirefighter(
            Long firefighterId
    ) {
        return firefighterRepository
                .findById(firefighterId)
                .orElseThrow(() ->
                        new FirefighterNotFoundException(
                                firefighterId
                        )
                );
    }

    private void validateActiveFirefighter(
            Firefighter firefighter
    ) {
        if (!firefighter.getUser().isActive()) {
            throw new InactiveFirefighterException(
                    firefighter.getId()
            );
        }
    }

    private void validateAvailability(
            Firefighter firefighter,
            LocalDate dutyDate
    ) {
        boolean unavailable =
                unavailabilityRepository
                        .existsByFirefighterIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                firefighter.getId(),
                                UnavailabilityStatus.APPROVED,
                                dutyDate,
                                dutyDate
                        );

        if (unavailable) {
            throw new FirefighterUnavailableForDutyException(
                    firefighter.getId(),
                    dutyDate
            );
        }
    }

    private void validateMandatoryRest(
            Firefighter firefighter,
            DutyAssignment currentAssignment,
            LocalDate dutyDate
    ) {
        boolean hasAdjacentDuty =
                dutyAssignmentRepository
                        .existsByFirefighterIdAndDutyDateBetweenAndIdNot(
                                firefighter.getId(),
                                dutyDate.minusDays(1),
                                dutyDate.plusDays(1),
                                currentAssignment.getId()
                        );

        if (hasAdjacentDuty) {
            throw new MandatoryRestViolationException(
                    firefighter.getId(),
                    dutyDate
            );
        }
    }

    private DutyAssignmentResponse toResponse(
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