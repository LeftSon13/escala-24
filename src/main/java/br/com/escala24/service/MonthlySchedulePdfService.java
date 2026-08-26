package br.com.escala24.service;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
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

@Service
public class MonthlySchedulePdfService {

    private static final Locale BRAZIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter GENERATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DUTY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final MonthlyScheduleExportDataService exportDataService;
    private final TemplateEngine templateEngine;
    private final Clock clock;

    @Autowired
    public MonthlySchedulePdfService(
            MonthlyScheduleExportDataService exportDataService,
            TemplateEngine templateEngine
    ) {
        this(
                exportDataService,
                templateEngine,
                Clock.system(ZoneId.of("America/Sao_Paulo"))
        );
    }

    MonthlySchedulePdfService(
            MonthlyScheduleExportDataService exportDataService,
            TemplateEngine templateEngine,
            Clock clock
    ) {
        this.exportDataService = exportDataService;
        this.templateEngine = templateEngine;
        this.clock = clock;
    }

    public byte[] exportPublishedSchedule(int year, int month) {
        return exportPublishedSchedule(
                year,
                month,
                MonthlySchedulePdfLayout.LIST
        );
    }

    public byte[] exportPublishedSchedule(
            int year,
            int month,
            MonthlySchedulePdfLayout layout
    ) {
        MonthlyScheduleGenerationResponse schedule =
                exportDataService.findPublished(year, month);

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
        context.setVariable("weeks", toCalendarWeeks(schedule));

        String html = templateEngine.process(
                layout == MonthlySchedulePdfLayout.CALENDAR
                        ? "pdf/monthly-schedule-calendar"
                        : "pdf/monthly-schedule",
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

    private List<List<CalendarDay>> toCalendarWeeks(
            MonthlyScheduleGenerationResponse schedule
    ) {
        YearMonth yearMonth = YearMonth.of(
                schedule.year(),
                schedule.month()
        );
        LocalDate firstCell = yearMonth.atDay(1).minusDays(
                yearMonth.atDay(1).getDayOfWeek().getValue() % 7
        );
        int leadingDays = yearMonth.atDay(1)
                .getDayOfWeek()
                .getValue() % 7;
        int weekCount = (int) Math.ceil(
                (leadingDays + yearMonth.lengthOfMonth()) / 7.0
        );

        return java.util.stream.IntStream.range(0, weekCount)
                .mapToObj(week -> java.util.stream.IntStream.range(0, 7)
                        .mapToObj(day -> firstCell.plusDays(week * 7L + day))
                        .map(date -> toCalendarDay(
                                date,
                                yearMonth,
                                schedule.assignments()
                        ))
                        .toList())
                .toList();
    }

    private CalendarDay toCalendarDay(
            LocalDate date,
            YearMonth yearMonth,
            List<DutyAssignmentResponse> assignments
    ) {
        DutyAssignmentResponse assignment = assignments.stream()
                .filter(item -> item.dutyDate().equals(date))
                .findFirst()
                .orElse(null);

        return new CalendarDay(
                date.getDayOfMonth(),
                !YearMonth.from(date).equals(yearMonth),
                date.getDayOfWeek() == DayOfWeek.SATURDAY
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY,
                assignment == null ? null : assignment.firefighterName(),
                assignment == null
                        ? null
                        : assignment.firefighterRegistration()
        );
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

    private record CalendarDay(
            int dayNumber,
            boolean outsideMonth,
            boolean weekend,
            String firefighterName,
            String firefighterRegistration
    ) {
    }
}
