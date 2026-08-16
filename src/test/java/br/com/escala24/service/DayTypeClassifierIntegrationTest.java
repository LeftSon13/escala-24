package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.escala24.IntegrationTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.DayType;
import br.com.escala24.entity.Holiday;
import br.com.escala24.repository.HolidayRepository;

@IntegrationTest
@Transactional
class DayTypeClassifierIntegrationTest {

    @Autowired
    private DayTypeClassifier dayTypeClassifier;

    @Autowired
    private HolidayRepository holidayRepository;

    @Test
    void shouldClassifyWeekdaysWeekendsAndHolidays() {
        LocalDate referenceDate = LocalDate.of(2092, 6, 1);

        LocalDate saturday = referenceDate.with(
                TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)
        );

        LocalDate sunday = saturday.plusDays(1);

        LocalDate commonWeekday = referenceDate.with(
                TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)
        );

        LocalDate weekdayHoliday = commonWeekday.plusDays(1);

        Holiday holiday = new Holiday();
        holiday.setDate(weekdayHoliday);
        holiday.setName("Feriado Nacional de Teste");

        holidayRepository.saveAndFlush(holiday);

        assertThat(dayTypeClassifier.classify(saturday))
                .isEqualTo(DayType.WEEKEND_OR_HOLIDAY);

        assertThat(dayTypeClassifier.classify(sunday))
                .isEqualTo(DayType.WEEKEND_OR_HOLIDAY);

        assertThat(dayTypeClassifier.classify(weekdayHoliday))
                .isEqualTo(DayType.WEEKEND_OR_HOLIDAY);

        assertThat(dayTypeClassifier.classify(commonWeekday))
                .isEqualTo(DayType.WEEKDAY);
    }
}
