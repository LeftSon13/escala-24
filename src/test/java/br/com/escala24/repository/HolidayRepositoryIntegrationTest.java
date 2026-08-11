package br.com.escala24.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.Holiday;

@SpringBootTest
@Transactional
class HolidayRepositoryIntegrationTest {

    @Autowired
    private HolidayRepository holidayRepository;

    @Test
    void shouldFindAndListHolidaysInChronologicalOrder() {
        Holiday independenceDay = new Holiday();
        independenceDay.setDate(
                LocalDate.of(2093, 9, 7)
        );
        independenceDay.setName(
                "Independência do Brasil"
        );

        Holiday newYear = new Holiday();
        newYear.setDate(
                LocalDate.of(2093, 1, 1)
        );
        newYear.setName(
                "Confraternização Universal"
        );

        holidayRepository.saveAllAndFlush(
                List.of(
                        independenceDay,
                        newYear
                )
        );

        Holiday savedHoliday = holidayRepository
                .findByDate(
                        LocalDate.of(2093, 9, 7)
                )
                .orElseThrow();

        assertThat(savedHoliday.getName())
                .isEqualTo(
                        "Independência do Brasil"
                );

        assertThat(savedHoliday.getCreatedAt())
                .isNotNull();

        assertThat(
                holidayRepository.existsByDate(
                        LocalDate.of(2093, 1, 1)
                )
        ).isTrue();

        assertThat(
                holidayRepository.existsByDate(
                        LocalDate.of(2093, 1, 2)
                )
        ).isFalse();

        List<Holiday> holidays = holidayRepository
                .findByDateBetweenOrderByDateAsc(
                        LocalDate.of(2093, 1, 1),
                        LocalDate.of(2093, 12, 31)
                );

        assertThat(holidays)
                .extracting(Holiday::getDate)
                .containsExactly(
                        LocalDate.of(2093, 1, 1),
                        LocalDate.of(2093, 9, 7)
                );
    }
}