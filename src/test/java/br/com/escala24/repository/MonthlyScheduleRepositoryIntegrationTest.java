package br.com.escala24.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.escala24.IntegrationTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.MonthlySchedule;
import br.com.escala24.entity.ScheduleStatus;

@IntegrationTest
@Transactional
class MonthlyScheduleRepositoryIntegrationTest {

    @Autowired
    private MonthlyScheduleRepository monthlyScheduleRepository;

    @Test
    void shouldFindMonthlyScheduleByYearAndMonth() {
        MonthlySchedule schedule = new MonthlySchedule();
        schedule.setYear(2098);
        schedule.setMonth(7);

        monthlyScheduleRepository.saveAndFlush(schedule);

        MonthlySchedule savedSchedule = monthlyScheduleRepository
                .findByYearAndMonth(2098, 7)
                .orElseThrow();

        assertThat(savedSchedule.getId()).isNotNull();
        assertThat(savedSchedule.getYear()).isEqualTo(2098);
        assertThat(savedSchedule.getMonth()).isEqualTo(7);
        assertThat(savedSchedule.getStatus())
                .isEqualTo(ScheduleStatus.DRAFT);
        assertThat(savedSchedule.getCreatedAt()).isNotNull();

        assertThat(monthlyScheduleRepository
                .existsByYearAndMonth(2098, 7))
                .isTrue();

        assertThat(monthlyScheduleRepository
                .existsByYearAndMonth(2098, 8))
                .isFalse();
    }
}
