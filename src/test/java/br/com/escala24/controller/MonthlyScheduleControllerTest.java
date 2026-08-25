package br.com.escala24.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.dto.DutyReassignmentRequest;
import br.com.escala24.dto.MonthlyScheduleGenerationRequest;
import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.DayType;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.exception.MandatoryRestViolationException;
import br.com.escala24.exception.MonthlyScheduleNotFoundException;
import br.com.escala24.service.DutyReassignmentService;
import br.com.escala24.service.MonthlyScheduleGenerationService;
import br.com.escala24.service.MonthlyScheduleManagementService;
import br.com.escala24.service.MonthlySchedulePdfService;

@WebMvcTest(MonthlyScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MonthlyScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonthlyScheduleGenerationService generationService;

    @MockitoBean
    private MonthlyScheduleManagementService managementService;

    @MockitoBean
    private DutyReassignmentService reassignmentService;

    @MockitoBean
    private MonthlySchedulePdfService pdfService;

    @Test
    void shouldGenerateMonthlySchedule() throws Exception {
        MonthlyScheduleGenerationResponse response = createScheduleResponse(1L, 2027, 8);

        when(generationService.generate(any()))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/monthly-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2027,
                                  "month": 8
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.year").value(2027))
                .andExpect(jsonPath("$.month").value(8))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(generationService).generate(
                new MonthlyScheduleGenerationRequest(
                        2027,
                        8));
    }

    @Test
    void shouldReturnBadRequestForInvalidMonth() throws Exception {
        when(managementService.findByYearAndMonth(2027, 13))
                .thenThrow(
                        new DateTimeException(
                                "Invalid value for MonthOfYear: 13"));

        mockMvc.perform(
                get("/api/monthly-schedules/2027/13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/monthly-schedules/2027/13"));
    }

    @Test
    void shouldFindMonthlySchedule() throws Exception {
        when(managementService.findByYearAndMonth(2027, 8))
                .thenReturn(createScheduleResponse(2L, 2027, 8));

        mockMvc.perform(
                get("/api/monthly-schedules/2027/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.year").value(2027))
                .andExpect(jsonPath("$.month").value(8));

        verify(managementService)
                .findByYearAndMonth(2027, 8);
    }

    @Test
    void shouldPublishMonthlySchedule() throws Exception {
        MonthlyScheduleGenerationResponse response = new MonthlyScheduleGenerationResponse(
                3L,
                2027,
                8,
                ScheduleStatus.PUBLISHED,
                LocalDateTime.of(2027, 7, 1, 10, 0),
                List.of());

        when(managementService.publish(2027, 8))
                .thenReturn(response);

        mockMvc.perform(
                post(
                        "/api/monthly-schedules/2027/8/publication"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("PUBLISHED"));

        verify(managementService).publish(2027, 8);
    }

    @Test
    void shouldExportPublishedScheduleAsPdf() throws Exception {
        byte[] pdf = "%PDF-1.7".getBytes();

        when(pdfService.exportPublishedSchedule(2027, 8))
                .thenReturn(pdf);

        mockMvc.perform(
                get("/api/monthly-schedules/2027/8/pdf"))
                .andExpect(status().isOk())
                .andExpect(
                        content().contentType(
                                MediaType.APPLICATION_PDF))
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                "attachment; filename=\"escala-2027-08.pdf\""))
                .andExpect(content().bytes(pdf));

        verify(pdfService).exportPublishedSchedule(2027, 8);
    }

    @Test
    void shouldReassignDuty() throws Exception {
        LocalDate dutyDate = LocalDate.of(2027, 8, 15);

        DutyAssignmentResponse response = new DutyAssignmentResponse(
                10L,
                dutyDate,
                dutyDate.atTime(8, 0),
                dutyDate.plusDays(1).atTime(8, 0),
                DayType.WEEKDAY,
                20L,
                "Bombeiro Substituto",
                "REG-20");

        when(reassignmentService.reassign(
                2027,
                8,
                dutyDate,
                new DutyReassignmentRequest(20L))).thenReturn(response);

        mockMvc.perform(
                put(
                        "/api/monthly-schedules/"
                                + "2027/8/assignments/2027-08-15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firefighterId": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(
                        jsonPath("$.dutyDate")
                                .value("2027-08-15"))
                .andExpect(
                        jsonPath("$.firefighterId")
                                .value(20));

        verify(reassignmentService).reassign(
                2027,
                8,
                dutyDate,
                new DutyReassignmentRequest(20L));
    }

    @Test
    void shouldReturnValidationErrors() throws Exception {
        mockMvc.perform(
                post("/api/monthly-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 0,
                                  "month": 13
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "A requisição possui campos inválidos"))
                .andExpect(
                        jsonPath("$.fieldErrors.year").exists())
                .andExpect(
                        jsonPath("$.fieldErrors.month").exists());
    }

    @Test
    void shouldReturnNotFoundError() throws Exception {
        when(managementService.findByYearAndMonth(2027, 8))
                .thenThrow(
                        new MonthlyScheduleNotFoundException(
                                2027,
                                8));

        mockMvc.perform(
                get("/api/monthly-schedules/2027/8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/monthly-schedules/2027/8"));
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(
                post("/api/monthly-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2027,
                                  "month":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "A requisição possui formato inválido"))
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/monthly-schedules"));
    }

    @Test
    void shouldReturnBusinessRuleError() throws Exception {
        LocalDate dutyDate = LocalDate.of(2027, 8, 15);

        when(reassignmentService.reassign(
                2027,
                8,
                dutyDate,
                new DutyReassignmentRequest(20L))).thenThrow(
                        new MandatoryRestViolationException(
                                20L,
                                dutyDate));

        mockMvc.perform(
                put(
                        "/api/monthly-schedules/"
                                + "2027/8/assignments/2027-08-15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firefighterId": 20
                                }
                                """))
                .andExpect(
                        status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(
                        jsonPath("$.error")
                                .value("Unprocessable Content"));
    }

    private MonthlyScheduleGenerationResponse createScheduleResponse(
            Long id,
            int year,
            int month) {
        return new MonthlyScheduleGenerationResponse(
                id,
                year,
                month,
                ScheduleStatus.DRAFT,
                LocalDateTime.of(year, 7, 1, 10, 0),
                List.of());
    }
}
