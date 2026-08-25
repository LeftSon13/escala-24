package br.com.escala24.service;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.entity.DayType;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.exception.UnpublishedScheduleExportException;

@Service
public class MonthlySchedulePdfService {

    private static final Locale BRAZIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter GENERATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DUTY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final MonthlyScheduleManagementService managementService;
    private final TemplateEngine templateEngine;
    private final Clock clock;

    @Autowired
    public MonthlySchedulePdfService(
            MonthlyScheduleManagementService managementService,
            TemplateEngine templateEngine
    ) {
        this(managementService, templateEngine, Clock.systemDefaultZone());
    }

    MonthlySchedulePdfService(
            MonthlyScheduleManagementService managementService,
            TemplateEngine templateEngine,
            Clock clock
    ) {
        this.managementService = managementService;
        this.templateEngine = templateEngine;
        this.clock = clock;
    }

    public byte[] exportPublishedSchedule(int year, int month) {
        MonthlyScheduleGenerationResponse schedule =
                managementService.findByYearAndMonth(year, month);

        if (schedule.status() != ScheduleStatus.PUBLISHED) {
            throw new UnpublishedScheduleExportException(year, month);
        }

        Context context = new Context(BRAZIL);
        context.setVariable("schedule", schedule);
        context.setVariable(
                "monthName",
                Month.of(month).getDisplayName(TextStyle.FULL, BRAZIL)
        );
        context.setVariable(
                "assignments",
                toPdfRows(schedule.assignments())
        );
        context.setVariable(
                "generatedAt",
                LocalDateTime.now(clock).format(GENERATED_AT_FORMAT)
        );

        String html = templateEngine.process(
                "pdf/monthly-schedule",
                context
        );

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Não foi possível gerar o PDF da escala",
                    exception
            );
        }
    }

    private List<PdfAssignmentRow> toPdfRows(
            List<DutyAssignmentResponse> assignments
    ) {
        return assignments.stream()
                .map(assignment -> new PdfAssignmentRow(
                        assignment.dutyDate().format(DATE_FORMAT),
                        assignment.dayType() == DayType.WEEKDAY
                                ? "Dia útil"
                                : "Fim de semana ou feriado",
                        assignment.firefighterName(),
                        assignment.firefighterRegistration(),
                        assignment.startDateTime()
                                .format(DUTY_TIME_FORMAT),
                        assignment.endDateTime()
                                .format(DUTY_TIME_FORMAT)
                ))
                .toList();
    }

    private record PdfAssignmentRow(
            String dutyDate,
            String dayType,
            String firefighterName,
            String firefighterRegistration,
            String startDateTime,
            String endDateTime
    ) {
    }
}
