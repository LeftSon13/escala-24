package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.HolidayRequest;
import br.com.escala24.dto.HolidayResponse;
import br.com.escala24.entity.DayType;
import br.com.escala24.exception.HolidayAlreadyExistsException;
import br.com.escala24.exception.HolidayNotFoundException;
import br.com.escala24.repository.HolidayRepository;

@SpringBootTest
@Transactional
class HolidayManagementServiceIntegrationTest {

    @Autowired
    private HolidayManagementService holidayService;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private DayTypeClassifier dayTypeClassifier;

    @Test
    void shouldCreateHolidayAndNormalizeName() {
        HolidayResponse response = holidayService.create(
                new HolidayRequest(
                        LocalDate.of(2094, 1, 1),
                        "  Confraternização Universal  "
                )
        );

        holidayRepository.flush();

        assertThat(response.id()).isNotNull();
        assertThat(response.date())
                .isEqualTo(LocalDate.of(2094, 1, 1));
        assertThat(response.name())
                .isEqualTo("Confraternização Universal");
        assertThat(response.createdAt()).isNotNull();

        assertThat(
                holidayRepository.existsByDate(
                        LocalDate.of(2094, 1, 1)
                )
        ).isTrue();
    }

    @Test
    void shouldListHolidaysByYearInChronologicalOrder() {
        holidayService.create(
                new HolidayRequest(
                        LocalDate.of(2094, 12, 25),
                        "Natal"
                )
        );

        holidayService.create(
                new HolidayRequest(
                        LocalDate.of(2094, 4, 21),
                        "Tiradentes"
                )
        );

        holidayService.create(
                new HolidayRequest(
                        LocalDate.of(2095, 1, 1),
                        "Confraternização Universal"
                )
        );

        List<HolidayResponse> holidays =
                holidayService.listByYear(2094);

        assertThat(holidays)
                .extracting(HolidayResponse::date)
                .containsExactly(
                        LocalDate.of(2094, 4, 21),
                        LocalDate.of(2094, 12, 25)
                );
    }

    @Test
    void shouldRejectDuplicatedHolidayDate() {
        LocalDate holidayDate = LocalDate.of(
                2094,
                9,
                7
        );

        holidayService.create(
                new HolidayRequest(
                        holidayDate,
                        "Independência do Brasil"
                )
        );

        assertThatThrownBy(
                () -> holidayService.create(
                        new HolidayRequest(
                                holidayDate,
                                "Outro feriado"
                        )
                )
        )
                .isInstanceOf(
                        HolidayAlreadyExistsException.class
                )
                .hasMessageContaining(
                        holidayDate.toString()
                );
    }

    @Test
    void shouldUpdateHolidayKeepingItsOwnDate() {
        HolidayResponse createdHoliday =
                holidayService.create(
                        new HolidayRequest(
                                LocalDate.of(2094, 5, 1),
                                "Nome inicial"
                        )
                );

        HolidayResponse updatedHoliday =
                holidayService.update(
                        createdHoliday.id(),
                        new HolidayRequest(
                                LocalDate.of(2094, 5, 1),
                                "  Dia do Trabalho  "
                        )
                );

        holidayRepository.flush();

        assertThat(updatedHoliday.id())
                .isEqualTo(createdHoliday.id());
        assertThat(updatedHoliday.date())
                .isEqualTo(LocalDate.of(2094, 5, 1));
        assertThat(updatedHoliday.name())
                .isEqualTo("Dia do Trabalho");

        assertThat(
                holidayRepository
                        .findById(createdHoliday.id())
                        .orElseThrow()
                        .getName()
        ).isEqualTo("Dia do Trabalho");
    }

    @Test
    void shouldRejectUpdateToDateUsedByAnotherHoliday() {
        HolidayResponse firstHoliday =
                holidayService.create(
                        new HolidayRequest(
                                LocalDate.of(2094, 6, 1),
                                "Primeiro feriado"
                        )
                );

        holidayService.create(
                new HolidayRequest(
                        LocalDate.of(2094, 6, 2),
                        "Segundo feriado"
                )
        );

        assertThatThrownBy(
                () -> holidayService.update(
                        firstHoliday.id(),
                        new HolidayRequest(
                                LocalDate.of(2094, 6, 2),
                                "Data duplicada"
                        )
                )
        )
                .isInstanceOf(
                        HolidayAlreadyExistsException.class
                );
    }

    @Test
    void shouldDeleteHoliday() {
        HolidayResponse createdHoliday =
                holidayService.create(
                        new HolidayRequest(
                                LocalDate.of(2094, 11, 15),
                                "Proclamação da República"
                        )
                );

        holidayService.delete(createdHoliday.id());
        holidayRepository.flush();

        assertThat(
                holidayRepository.existsById(
                        createdHoliday.id()
                )
        ).isFalse();
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingHoliday() {
        assertThatThrownBy(
                () -> holidayService.delete(
                        Long.MAX_VALUE
                )
        )
                .isInstanceOf(
                        HolidayNotFoundException.class
                )
                .hasMessageContaining(
                        Long.toString(Long.MAX_VALUE)
                );
    }

    @Test
    void shouldClassifyRegisteredWeekdayAsHoliday() {
        LocalDate holidayDate = LocalDate.of(
                2094,
                3,
                15
        );

        assertThat(holidayDate.getDayOfWeek())
                .isNotIn(
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                );

        assertThat(dayTypeClassifier.classify(holidayDate))
                .isEqualTo(DayType.WEEKDAY);

        holidayService.create(
                new HolidayRequest(
                        holidayDate,
                        "Feriado de teste"
                )
        );

        holidayRepository.flush();

        assertThat(dayTypeClassifier.classify(holidayDate))
                .isEqualTo(
                        DayType.WEEKEND_OR_HOLIDAY
                );
    }
}