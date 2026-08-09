package br.com.escala24.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.DayType;
import br.com.escala24.entity.DutyAssignment;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.MonthlySchedule;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;

@SpringBootTest
@Transactional
class DutyAssignmentRepositoryIntegrationTest {

    @Autowired
    private DutyAssignmentRepository dutyAssignmentRepository;

    @Autowired
    private MonthlyScheduleRepository monthlyScheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirefighterRepository firefighterRepository;

    @Test
    void shouldQueryAssignmentsAndCountSpecialDutiesAcrossMonths() {
        User user = new User();
        user.setName("Bombeiro Histórico");
        user.setEmail("history@escala24.com");
        user.setPassword("encoded-password");
        user.setRole(Role.FIREFIGHTER);
        user.setActive(true);
        user.setMustChangePassword(true);

        userRepository.saveAndFlush(user);

        Firefighter firefighter = new Firefighter();
        firefighter.setUser(user);
        firefighter.setRegistration("REG-HISTORY-001");
        firefighter.setPhone("11999999998");

        firefighterRepository.saveAndFlush(firefighter);

        MonthlySchedule januarySchedule = new MonthlySchedule();
        januarySchedule.setYear(2097);
        januarySchedule.setMonth(1);

        MonthlySchedule februarySchedule = new MonthlySchedule();
        februarySchedule.setYear(2097);
        februarySchedule.setMonth(2);

        monthlyScheduleRepository.saveAllAndFlush(
                List.of(januarySchedule, februarySchedule)
        );

        DutyAssignment januarySpecialDuty = createAssignment(
                januarySchedule,
                firefighter,
                LocalDate.of(2097, 1, 4),
                DayType.WEEKEND_OR_HOLIDAY
        );

        DutyAssignment januaryWeekdayDuty = createAssignment(
                januarySchedule,
                firefighter,
                LocalDate.of(2097, 1, 15),
                DayType.WEEKDAY
        );

        DutyAssignment februarySpecialDuty = createAssignment(
                februarySchedule,
                firefighter,
                LocalDate.of(2097, 2, 8),
                DayType.WEEKEND_OR_HOLIDAY
        );

        dutyAssignmentRepository.saveAllAndFlush(
                List.of(
                        januaryWeekdayDuty,
                        februarySpecialDuty,
                        januarySpecialDuty
                )
        );

        List<DutyAssignment> januaryAssignments =
                dutyAssignmentRepository
                        .findByMonthlyScheduleIdOrderByDutyDateAsc(
                                januarySchedule.getId()
                        );

        assertThat(januaryAssignments)
                .extracting(DutyAssignment::getDutyDate)
                .containsExactly(
                        LocalDate.of(2097, 1, 4),
                        LocalDate.of(2097, 1, 15)
                );

        assertThat(januaryAssignments.getFirst()
                .getFirefighter()
                .getUser()
                .getName())
                .isEqualTo("Bombeiro Histórico");

        assertThat(dutyAssignmentRepository
                .existsByMonthlyScheduleIdAndDutyDate(
                        januarySchedule.getId(),
                        LocalDate.of(2097, 1, 4)
                ))
                .isTrue();

        assertThat(dutyAssignmentRepository
                .existsByMonthlyScheduleIdAndDutyDate(
                        januarySchedule.getId(),
                        LocalDate.of(2097, 1, 5)
                ))
                .isFalse();

        DutyAssignment previousAssignment = dutyAssignmentRepository
                .findTopByFirefighterIdAndDutyDateBeforeOrderByDutyDateDesc(
                        firefighter.getId(),
                        LocalDate.of(2097, 3, 1)
                )
                .orElseThrow();

        assertThat(previousAssignment.getDutyDate())
                .isEqualTo(LocalDate.of(2097, 2, 8));

        long specialDutyCount = dutyAssignmentRepository
                .countByFirefighterIdAndDayTypeAndDutyDateBetween(
                        firefighter.getId(),
                        DayType.WEEKEND_OR_HOLIDAY,
                        LocalDate.of(2097, 1, 1),
                        LocalDate.of(2097, 12, 31)
                );

        long weekdayDutyCount = dutyAssignmentRepository
                .countByFirefighterIdAndDayTypeAndDutyDateBetween(
                        firefighter.getId(),
                        DayType.WEEKDAY,
                        LocalDate.of(2097, 1, 1),
                        LocalDate.of(2097, 12, 31)
                );

        assertThat(specialDutyCount).isEqualTo(2);
        assertThat(weekdayDutyCount).isEqualTo(1);
    }

    private DutyAssignment createAssignment(
            MonthlySchedule schedule,
            Firefighter firefighter,
            LocalDate dutyDate,
            DayType dayType
    ) {
        DutyAssignment assignment = new DutyAssignment();
        assignment.setMonthlySchedule(schedule);
        assignment.setFirefighter(firefighter);
        assignment.setDutyDate(dutyDate);
        assignment.setDayType(dayType);
        return assignment;
    }
}