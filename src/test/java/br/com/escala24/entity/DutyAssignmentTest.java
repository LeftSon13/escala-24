package br.com.escala24.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class DutyAssignmentTest {

    @Test
    void shouldCalculateTwentyFourHourDutyPeriod() {
        DutyAssignment assignment = new DutyAssignment();
        assignment.setDutyDate(LocalDate.of(2026, 8, 10));

        assertThat(assignment.getStartDateTime())
                .isEqualTo(LocalDateTime.of(2026, 8, 10, 8, 0));

        assertThat(assignment.getEndDateTime())
                .isEqualTo(LocalDateTime.of(2026, 8, 11, 8, 0));
    }
}