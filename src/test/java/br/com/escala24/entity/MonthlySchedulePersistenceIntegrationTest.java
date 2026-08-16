package br.com.escala24.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import br.com.escala24.IntegrationTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@IntegrationTest
@Transactional
class MonthlySchedulePersistenceIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldPersistMonthlyScheduleWithDutyAssignment() {
        User user = new User();
        user.setName("Bombeiro da Escala");
        user.setEmail("schedule-model@escala24.com");
        user.setPassword("encoded-password");
        user.setRole(Role.FIREFIGHTER);
        user.setActive(true);
        user.setMustChangePassword(true);

        entityManager.persist(user);

        Firefighter firefighter = new Firefighter();
        firefighter.setUser(user);
        firefighter.setRegistration("REG-SCHEDULE-001");
        firefighter.setPhone("11999999999");

        entityManager.persist(firefighter);

        MonthlySchedule schedule = new MonthlySchedule();
        schedule.setYear(2099);
        schedule.setMonth(8);

        entityManager.persist(schedule);

        DutyAssignment assignment = new DutyAssignment();
        assignment.setMonthlySchedule(schedule);
        assignment.setFirefighter(firefighter);
        assignment.setDutyDate(LocalDate.of(2099, 8, 10));
        assignment.setDayType(DayType.WEEKEND_OR_HOLIDAY);

        entityManager.persist(assignment);
        entityManager.flush();

        Long assignmentId = assignment.getId();

        entityManager.clear();

        DutyAssignment savedAssignment =
                entityManager.find(DutyAssignment.class, assignmentId);

        assertThat(savedAssignment).isNotNull();
        assertThat(savedAssignment.getDutyDate())
                .isEqualTo(LocalDate.of(2099, 8, 10));
        assertThat(savedAssignment.getDayType())
                .isEqualTo(DayType.WEEKEND_OR_HOLIDAY);

        assertThat(savedAssignment.getMonthlySchedule().getYear())
                .isEqualTo(2099);
        assertThat(savedAssignment.getMonthlySchedule().getMonth())
                .isEqualTo(8);
        assertThat(savedAssignment.getMonthlySchedule().getStatus())
                .isEqualTo(ScheduleStatus.DRAFT);
        assertThat(savedAssignment.getMonthlySchedule().getCreatedAt())
                .isNotNull();

        assertThat(savedAssignment.getFirefighter().getRegistration())
                .isEqualTo("REG-SCHEDULE-001");

        assertThat(savedAssignment.getStartDateTime())
                .isEqualTo(LocalDateTime.of(2099, 8, 10, 8, 0));
        assertThat(savedAssignment.getEndDateTime())
                .isEqualTo(LocalDateTime.of(2099, 8, 11, 8, 0));
    }
}
