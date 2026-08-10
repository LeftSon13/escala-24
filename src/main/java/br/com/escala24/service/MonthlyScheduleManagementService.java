package br.com.escala24.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.DutyAssignment;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.MonthlySchedule;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.exception.IncompleteMonthlyScheduleException;
import br.com.escala24.exception.MonthlyScheduleAlreadyPublishedException;
import br.com.escala24.exception.MonthlyScheduleNotFoundException;
import br.com.escala24.repository.DutyAssignmentRepository;
import br.com.escala24.repository.MonthlyScheduleRepository;

@Service
public class MonthlyScheduleManagementService {

    private final MonthlyScheduleRepository monthlyScheduleRepository;
    private final DutyAssignmentRepository dutyAssignmentRepository;

    public MonthlyScheduleManagementService(
            MonthlyScheduleRepository monthlyScheduleRepository,
            DutyAssignmentRepository dutyAssignmentRepository
    ) {
        this.monthlyScheduleRepository = monthlyScheduleRepository;
        this.dutyAssignmentRepository = dutyAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyScheduleGenerationResponse findByYearAndMonth(
            int year,
            int month
    ) {
        YearMonth requestedMonth = YearMonth.of(year, month);

        MonthlySchedule schedule = findSchedule(requestedMonth);

        List<DutyAssignment> assignments =
                findAssignments(schedule);

        return toResponse(schedule, assignments);
    }

    @Transactional
    public MonthlyScheduleGenerationResponse publish(
            int year,
            int month
    ) {
        YearMonth requestedMonth = YearMonth.of(year, month);

        MonthlySchedule schedule = findSchedule(requestedMonth);

        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            throw new MonthlyScheduleAlreadyPublishedException(
                    year,
                    month
            );
        }

        List<DutyAssignment> assignments =
                findAssignments(schedule);

        validateCompleteCoverage(
                requestedMonth,
                assignments
        );

        schedule.setStatus(ScheduleStatus.PUBLISHED);

        MonthlySchedule publishedSchedule =
                monthlyScheduleRepository.saveAndFlush(schedule);

        return toResponse(
                publishedSchedule,
                assignments
        );
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

    private List<DutyAssignment> findAssignments(
            MonthlySchedule schedule
    ) {
        return dutyAssignmentRepository
                .findByMonthlyScheduleIdOrderByDutyDateAsc(
                        schedule.getId()
                );
    }

    private void validateCompleteCoverage(
            YearMonth requestedMonth,
            List<DutyAssignment> assignments
    ) {
        LocalDate firstDate = requestedMonth.atDay(1);
        LocalDate lastDate = requestedMonth.atEndOfMonth();

        Set<LocalDate> expectedDates =
                firstDate
                        .datesUntil(lastDate.plusDays(1))
                        .collect(Collectors.toSet());

        Set<LocalDate> coveredDates =
                assignments.stream()
                        .map(DutyAssignment::getDutyDate)
                        .filter(expectedDates::contains)
                        .collect(Collectors.toSet());

        if (!coveredDates.equals(expectedDates)) {
            throw new IncompleteMonthlyScheduleException(
                    requestedMonth.getYear(),
                    requestedMonth.getMonthValue(),
                    expectedDates.size(),
                    coveredDates.size()
            );
        }
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