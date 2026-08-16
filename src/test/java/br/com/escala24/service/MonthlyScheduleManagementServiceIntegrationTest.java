package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.escala24.IntegrationTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.MonthlyScheduleGenerationRequest;
import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.MonthlySchedule;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.entity.User;
import br.com.escala24.exception.IncompleteMonthlyScheduleException;
import br.com.escala24.exception.MonthlyScheduleAlreadyPublishedException;
import br.com.escala24.exception.MonthlyScheduleNotFoundException;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.MonthlyScheduleRepository;
import br.com.escala24.repository.UserRepository;

@IntegrationTest
@Transactional
class MonthlyScheduleManagementServiceIntegrationTest {

    @Autowired
    private MonthlyScheduleManagementService managementService;

    @Autowired
    private MonthlyScheduleGenerationService generationService;

    @Autowired
    private MonthlyScheduleRepository monthlyScheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirefighterRepository firefighterRepository;

    @Test
    void shouldFindAndPublishCompleteMonthlySchedule() {
        createFirefighter(1);
        createFirefighter(2);
        createFirefighter(3);

        YearMonth requestedMonth = YearMonth.of(2085, 2);

        MonthlyScheduleGenerationResponse generated =
                generationService.generate(
                        new MonthlyScheduleGenerationRequest(
                                requestedMonth.getYear(),
                                requestedMonth.getMonthValue()
                        )
                );

        MonthlyScheduleGenerationResponse found =
                managementService.findByYearAndMonth(
                        requestedMonth.getYear(),
                        requestedMonth.getMonthValue()
                );

        assertThat(found.id()).isEqualTo(generated.id());
        assertThat(found.status())
                .isEqualTo(ScheduleStatus.DRAFT);
        assertThat(found.assignments())
                .hasSize(requestedMonth.lengthOfMonth());

        MonthlyScheduleGenerationResponse published =
                managementService.publish(
                        requestedMonth.getYear(),
                        requestedMonth.getMonthValue()
                );

        assertThat(published.status())
                .isEqualTo(ScheduleStatus.PUBLISHED);
        assertThat(published.assignments())
                .hasSize(requestedMonth.lengthOfMonth());

        MonthlySchedule savedSchedule =
                monthlyScheduleRepository
                        .findByYearAndMonth(
                                requestedMonth.getYear(),
                                requestedMonth.getMonthValue()
                        )
                        .orElseThrow();

        assertThat(savedSchedule.getStatus())
                .isEqualTo(ScheduleStatus.PUBLISHED);
    }

    @Test
    void shouldRejectMissingMonthlySchedule() {
        assertThatThrownBy(() ->
                managementService.findByYearAndMonth(
                        2084,
                        12
                )
        )
                .isInstanceOf(
                        MonthlyScheduleNotFoundException.class
                )
                .hasMessageContaining("12")
                .hasMessageContaining("2084");
    }

    @Test
    void shouldRejectPublishingIncompleteMonthlySchedule() {
        MonthlySchedule incompleteSchedule =
                new MonthlySchedule();

        incompleteSchedule.setYear(2083);
        incompleteSchedule.setMonth(1);

        monthlyScheduleRepository.saveAndFlush(
                incompleteSchedule
        );

        assertThatThrownBy(() ->
                managementService.publish(
                        2083,
                        1
                )
        )
                .isInstanceOf(
                        IncompleteMonthlyScheduleException.class
                )
                .hasMessageContaining("31")
                .hasMessageContaining("0");

        assertThat(incompleteSchedule.getStatus())
                .isEqualTo(ScheduleStatus.DRAFT);
    }

    @Test
    void shouldRejectPublishingAlreadyPublishedSchedule() {
        MonthlySchedule publishedSchedule =
                new MonthlySchedule();

        publishedSchedule.setYear(2082);
        publishedSchedule.setMonth(6);
        publishedSchedule.setStatus(ScheduleStatus.PUBLISHED);

        monthlyScheduleRepository.saveAndFlush(
                publishedSchedule
        );

        assertThatThrownBy(() ->
                managementService.publish(
                        2082,
                        6
                )
        )
                .isInstanceOf(
                        MonthlyScheduleAlreadyPublishedException.class
                )
                .hasMessageContaining("6")
                .hasMessageContaining("2082");
    }

    private Firefighter createFirefighter(int number) {
        User user = new User();
        user.setName("Bombeiro de Gestão " + number);
        user.setEmail(
                "schedule-management-"
                        + number
                        + "@escala24.com"
        );
        user.setPassword("encoded-password");
        user.setRole(Role.FIREFIGHTER);
        user.setActive(true);
        user.setMustChangePassword(true);

        userRepository.saveAndFlush(user);

        Firefighter firefighter = new Firefighter();
        firefighter.setUser(user);
        firefighter.setRegistration(
                "REG-SCHEDULE-MANAGEMENT-" + number
        );
        firefighter.setPhone("1188888888" + number);

        return firefighterRepository.saveAndFlush(firefighter);
    }
}
