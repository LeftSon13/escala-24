package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;

import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.DayType;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.exception.UnpublishedScheduleExportException;

@ExtendWith(MockitoExtension.class)
class MonthlySchedulePdfServiceTest {

    @Mock
    private MonthlyScheduleExportDataService exportDataService;

    @Mock
    private TemplateEngine templateEngine;

    @Test
    void shouldRejectDraftScheduleExport() {
        when(exportDataService.findPublished(2027, 8))
                .thenThrow(new UnpublishedScheduleExportException(2027, 8));

        MonthlySchedulePdfService service = createService();

        assertThatThrownBy(() ->
                service.exportPublishedSchedule(2027, 8))
                .isInstanceOf(
                        UnpublishedScheduleExportException.class)
                .hasMessageContaining("precisa estar publicada");
    }

    @Test
    void shouldGeneratePdfForPublishedSchedule() {
        when(exportDataService.findPublished(2027, 8))
                .thenReturn(schedule(ScheduleStatus.PUBLISHED));
        when(templateEngine.process(
                eq("pdf/monthly-schedule"),
                any(IContext.class)))
                .thenReturn("""
                        <!DOCTYPE html>
                        <html><head><title>Escala</title></head>
                        <body><p>Escala publicada</p></body></html>
                        """);

        byte[] pdf = createService()
                .exportPublishedSchedule(2027, 8);

        assertThat(pdf)
                .isNotEmpty()
                .startsWith("%PDF".getBytes());
        verify(templateEngine).process(
                eq("pdf/monthly-schedule"),
                any(IContext.class));
    }

    @Test
    void shouldRenderCompleteMonthlyScheduleTemplate() throws Exception {
        MonthlyScheduleGenerationResponse completeSchedule =
                completeSchedule();
        when(exportDataService.findPublished(2027, 8))
                .thenReturn(completeSchedule);

        byte[] pdf = new MonthlySchedulePdfService(
                exportDataService,
                realTemplateEngine(),
                fixedClock()
        ).exportPublishedSchedule(2027, 8);

        Path outputDirectory = Path.of("target", "pdf-qa");
        Files.createDirectories(outputDirectory);
        Files.write(
                outputDirectory.resolve("escala-2027-08.pdf"),
                pdf
        );

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            PDFRenderer renderer = new PDFRenderer(document);
            Path renderedDirectory = outputDirectory.resolve("rendered");
            Files.createDirectories(renderedDirectory);

            for (int page = 0;
                    page < document.getNumberOfPages();
                    page++) {
                ImageIO.write(
                        renderer.renderImageWithDPI(page, 120),
                        "png",
                        renderedDirectory
                                .resolve("page-%d.png".formatted(page + 1))
                                .toFile()
                );
            }

            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(text)
                    .contains("Escala mensal de plantões")
                    .contains("Bombeiro Operacional 31")
                    .contains("REG-031");
        }
    }

    @Test
    void shouldRenderCalendarLayoutOnOneLandscapePage() throws Exception {
        when(exportDataService.findPublished(2027, 8))
                .thenReturn(completeSchedule());

        byte[] pdf = new MonthlySchedulePdfService(
                exportDataService,
                realTemplateEngine(),
                fixedClock()
        ).exportPublishedSchedule(
                2027,
                8,
                MonthlySchedulePdfLayout.CALENDAR
        );

        Path outputDirectory = Path.of("target", "pdf-qa", "calendar");
        Files.createDirectories(outputDirectory);
        Files.write(
                outputDirectory.resolve("escala-2027-08-calendar.pdf"),
                pdf
        );

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            ImageIO.write(
                    new PDFRenderer(document).renderImageWithDPI(0, 140),
                    "png",
                    outputDirectory.resolve("page-1.png").toFile()
            );

            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(document.getPage(0).getMediaBox().getWidth())
                    .isGreaterThan(document.getPage(0).getMediaBox().getHeight());
            assertThat(text)
                    .contains("Domingo")
                    .contains("Bombeiro Operacional 31")
                    .contains("REG-031");
        }
    }

    private MonthlySchedulePdfService createService() {
        return new MonthlySchedulePdfService(
                exportDataService,
                templateEngine,
                fixedClock()
        );
    }

    private Clock fixedClock() {
        return Clock.fixed(
                Instant.parse("2027-08-01T13:30:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );
    }

    private TemplateEngine realTemplateEngine() {
        ClassLoaderTemplateResolver resolver =
                new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private MonthlyScheduleGenerationResponse completeSchedule() {
        List<DutyAssignmentResponse> assignments =
                IntStream.rangeClosed(1, 31)
                        .mapToObj(day -> assignment(day))
                        .toList();

        return new MonthlyScheduleGenerationResponse(
                1L,
                2027,
                8,
                ScheduleStatus.PUBLISHED,
                LocalDateTime.of(2027, 7, 20, 10, 0),
                assignments
        );
    }

    private DutyAssignmentResponse assignment(int day) {
        LocalDate dutyDate = LocalDate.of(2027, 8, day);

        return new DutyAssignmentResponse(
                (long) day,
                dutyDate,
                dutyDate.atTime(8, 0),
                dutyDate.plusDays(1).atTime(8, 0),
                day % 7 == 0
                        ? DayType.WEEKEND_OR_HOLIDAY
                        : DayType.WEEKDAY,
                (long) day,
                "Bombeiro Operacional " + day,
                "REG-%03d".formatted(day)
        );
    }

    private MonthlyScheduleGenerationResponse schedule(
            ScheduleStatus status
    ) {
        LocalDate dutyDate = LocalDate.of(2027, 8, 1);
        DutyAssignmentResponse assignment =
                new DutyAssignmentResponse(
                        1L,
                        dutyDate,
                        dutyDate.atTime(8, 0),
                        dutyDate.plusDays(1).atTime(8, 0),
                        DayType.WEEKDAY,
                        2L,
                        "Bombeiro Teste",
                        "REG-2"
                );

        return new MonthlyScheduleGenerationResponse(
                1L,
                2027,
                8,
                status,
                LocalDateTime.of(2027, 7, 20, 10, 0),
                List.of(assignment)
        );
    }
}
