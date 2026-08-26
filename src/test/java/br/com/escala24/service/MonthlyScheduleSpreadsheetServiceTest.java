package br.com.escala24.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.DayType;
import br.com.escala24.entity.ScheduleStatus;

@ExtendWith(MockitoExtension.class)
class MonthlyScheduleSpreadsheetServiceTest {

    @Mock
    private MonthlyScheduleExportDataService exportDataService;

    @Test
    void shouldGenerateReadableSpreadsheetWithTypedDates() throws Exception {
        when(exportDataService.findPublished(2027, 8))
                .thenReturn(schedule());

        byte[] spreadsheet = new MonthlyScheduleSpreadsheetService(
                exportDataService
        ).exportPublishedSchedule(2027, 8);

        Path outputDirectory = Path.of("target", "spreadsheet-qa");
        Files.createDirectories(outputDirectory);
        Files.write(
                outputDirectory.resolve("escala-2027-08.xlsx"),
                spreadsheet
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(spreadsheet)
        )) {
            var sheet = workbook.getSheet("Escala mensal");

            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue())
                    .isEqualTo("Data");
            assertThat(sheet.getRow(4).getCell(0).getCellType())
                    .isEqualTo(CellType.NUMERIC);
            assertThat(sheet.getRow(4).getCell(2).getStringCellValue())
                    .isEqualTo("Bombeiro Teste");
            assertThat(sheet.getPaneInformation()).isNotNull();
            assertThat(sheet.getCTWorksheet().isSetAutoFilter()).isTrue();
            assertThat(sheet.getLastRowNum()).isEqualTo(4);
        }
    }

    private MonthlyScheduleGenerationResponse schedule() {
        LocalDate date = LocalDate.of(2027, 8, 1);
        DutyAssignmentResponse assignment = new DutyAssignmentResponse(
                1L,
                date,
                date.atTime(8, 0),
                date.plusDays(1).atTime(8, 0),
                DayType.WEEKEND_OR_HOLIDAY,
                2L,
                "Bombeiro Teste",
                "REG-2"
        );

        return new MonthlyScheduleGenerationResponse(
                1L,
                2027,
                8,
                ScheduleStatus.PUBLISHED,
                LocalDateTime.of(2027, 7, 20, 10, 0),
                List.of(assignment)
        );
    }
}
