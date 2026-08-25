package br.com.escala24.service;

import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.DayType;

@Service
public class MonthlyScheduleSpreadsheetService {

    private static final String[] HEADERS = {
        "Data",
        "Classificação",
        "Bombeiro",
        "Matrícula",
        "Início",
        "Término"
    };

    private final MonthlyScheduleExportDataService exportDataService;

    public MonthlyScheduleSpreadsheetService(
            MonthlyScheduleExportDataService exportDataService
    ) {
        this.exportDataService = exportDataService;
    }

    public byte[] exportPublishedSchedule(int year, int month) {
        MonthlyScheduleGenerationResponse schedule =
                exportDataService.findPublished(year, month);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Escala mensal");
            createTitle(workbook, sheet, year, month);
            createHeaders(workbook, sheet);
            createAssignments(workbook, sheet, schedule);
            configureSheet(sheet, schedule.assignments().size());

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Não foi possível gerar a planilha da escala",
                    exception
            );
        }
    }

    private void createTitle(
            XSSFWorkbook workbook,
            Sheet sheet,
            int year,
            int month
    ) {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        var titleCell = titleRow.createCell(0);
        titleCell.setCellValue(
                "Escala mensal de plantões - %02d/%04d".formatted(
                        month,
                        year
                )
        );

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFillForegroundColor(
                IndexedColors.DARK_BLUE.getIndex()
        );
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        Row statusRow = sheet.createRow(1);
        statusRow.createCell(0).setCellValue("Status: Publicada");
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));
    }

    private void createHeaders(XSSFWorkbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(3);
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        for (int index = 0; index < HEADERS.length; index++) {
            var cell = headerRow.createCell(index);
            cell.setCellValue(HEADERS[index]);
            cell.setCellStyle(style);
        }
    }

    private void createAssignments(
            XSSFWorkbook workbook,
            Sheet sheet,
            MonthlyScheduleGenerationResponse schedule
    ) {
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(
                workbook.createDataFormat().getFormat("dd/mm/yyyy")
        );
        CellStyle dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(
                workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm")
        );

        for (int index = 0;
                index < schedule.assignments().size();
                index++) {
            DutyAssignmentResponse assignment =
                    schedule.assignments().get(index);
            Row row = sheet.createRow(index + 4);

            var dateCell = row.createCell(0);
            dateCell.setCellValue(assignment.dutyDate());
            dateCell.setCellStyle(dateStyle);
            row.createCell(1).setCellValue(
                    assignment.dayType() == DayType.WEEKDAY
                            ? "Dia útil"
                            : "Fim de semana ou feriado"
            );
            row.createCell(2).setCellValue(assignment.firefighterName());
            row.createCell(3).setCellValue(
                    assignment.firefighterRegistration()
            );
            var startCell = row.createCell(4);
            startCell.setCellValue(assignment.startDateTime());
            startCell.setCellStyle(dateTimeStyle);
            var endCell = row.createCell(5);
            endCell.setCellValue(assignment.endDateTime());
            endCell.setCellStyle(dateTimeStyle);
        }
    }

    private void configureSheet(Sheet sheet, int assignmentCount) {
        sheet.createFreezePane(0, 4);
        sheet.setAutoFilter(new CellRangeAddress(
                3,
                3 + assignmentCount,
                0,
                HEADERS.length - 1
        ));
        sheet.setDisplayGridlines(false);
        sheet.setColumnWidth(0, 14 * 256);
        sheet.setColumnWidth(1, 29 * 256);
        sheet.setColumnWidth(2, 30 * 256);
        sheet.setColumnWidth(3, 16 * 256);
        sheet.setColumnWidth(4, 21 * 256);
        sheet.setColumnWidth(5, 21 * 256);
    }
}
