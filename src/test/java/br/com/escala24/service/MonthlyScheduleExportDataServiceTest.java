package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.exception.UnpublishedScheduleExportException;

@ExtendWith(MockitoExtension.class)
class MonthlyScheduleExportDataServiceTest {

    @Mock
    private MonthlyScheduleManagementService managementService;

    @Test
    void shouldReturnPublishedSchedule() {
        MonthlyScheduleGenerationResponse schedule = schedule(
                ScheduleStatus.PUBLISHED
        );
        when(managementService.findByYearAndMonth(2027, 8))
                .thenReturn(schedule);

        MonthlyScheduleGenerationResponse result = service()
                .findPublished(2027, 8);

        assertThat(result).isSameAs(schedule);
    }

    @Test
    void shouldRejectDraftSchedule() {
        when(managementService.findByYearAndMonth(2027, 8))
                .thenReturn(schedule(ScheduleStatus.DRAFT));

        assertThatThrownBy(() -> service().findPublished(2027, 8))
                .isInstanceOf(UnpublishedScheduleExportException.class)
                .hasMessageContaining("precisa estar publicada");
    }

    private MonthlyScheduleExportDataService service() {
        return new MonthlyScheduleExportDataService(managementService);
    }

    private MonthlyScheduleGenerationResponse schedule(
            ScheduleStatus status
    ) {
        return new MonthlyScheduleGenerationResponse(
                1L,
                2027,
                8,
                status,
                LocalDateTime.of(2027, 7, 20, 10, 0),
                List.of()
        );
    }
}
