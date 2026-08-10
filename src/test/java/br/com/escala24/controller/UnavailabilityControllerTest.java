package br.com.escala24.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
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

import br.com.escala24.dto.UnavailabilityRequest;
import br.com.escala24.dto.UnavailabilityResponse;
import br.com.escala24.dto.UnavailabilityReviewResponse;
import br.com.escala24.entity.UnavailabilityStatus;
import br.com.escala24.entity.UnavailabilityType;
import br.com.escala24.exception.InvalidUnavailabilityPeriodException;
import br.com.escala24.exception.UnavailabilityAlreadyReviewedException;
import br.com.escala24.exception.UnavailabilityNotFoundException;
import br.com.escala24.service.UnavailabilityManagementService;

@WebMvcTest(UnavailabilityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UnavailabilityControllerTest {

    private static final String FIREFIGHTER_EMAIL = "requester-api@escala24.com";

    private static final String ADMIN_EMAIL = "reviewer-api@escala24.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnavailabilityManagementService managementService;

    @Test
    void shouldRequestUnavailability() throws Exception {
        when(managementService
                .requestForAuthenticatedFirefighter(
                        any(),
                        any()))
                .thenReturn(createResponse(
                        1L,
                        UnavailabilityStatus.PENDING));

        mockMvc.perform(
                post("/api/unavailabilities")
                        .principal(principal(
                                FIREFIGHTER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING"));

        verify(managementService)
                .requestForAuthenticatedFirefighter(
                        FIREFIGHTER_EMAIL,
                        new UnavailabilityRequest(
                                UnavailabilityType.PERSONAL_COMMITMENT,
                                LocalDate.of(2030, 3, 10),
                                LocalDate.of(2030, 3, 12),
                                "Compromisso familiar"));
    }

    @Test
    void shouldListOwnUnavailabilities()
            throws Exception {
        when(managementService
                .listForAuthenticatedFirefighter(
                        FIREFIGHTER_EMAIL))
                .thenReturn(List.of(
                        createResponse(
                                2L,
                                UnavailabilityStatus.PENDING)));

        mockMvc.perform(
                get("/api/unavailabilities/me")
                        .principal(principal(
                                FIREFIGHTER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2));

        verify(managementService)
                .listForAuthenticatedFirefighter(
                        FIREFIGHTER_EMAIL);
    }

    @Test
    void shouldListPendingUnavailabilities()
            throws Exception {
        when(managementService.listPending())
                .thenReturn(List.of(
                        new UnavailabilityReviewResponse(
                                3L,
                                20L,
                                "Bombeiro Solicitante",
                                UnavailabilityType.PERSONAL_COMMITMENT,
                                LocalDate.of(2030, 3, 10),
                                LocalDate.of(2030, 3, 12),
                                "Compromisso familiar",
                                UnavailabilityStatus.PENDING,
                                LocalDateTime.of(
                                        2030,
                                        2,
                                        1,
                                        10,
                                        0))));

        mockMvc.perform(
                get("/api/unavailabilities/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].reason")
                                .value(
                                        "Compromisso familiar"));
    }

    @Test
    void shouldApproveUnavailability()
            throws Exception {
        when(managementService
                .approveForAuthenticatedAdministrator(
                        4L,
                        ADMIN_EMAIL))
                .thenReturn(createResponse(
                        4L,
                        UnavailabilityStatus.APPROVED));

        mockMvc.perform(
                patch(
                        "/api/unavailabilities/4/approval")
                        .principal(principal(ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("APPROVED"));

        verify(managementService)
                .approveForAuthenticatedAdministrator(
                        4L,
                        ADMIN_EMAIL);
    }

    @Test
    void shouldRejectUnavailability()
            throws Exception {
        when(managementService
                .rejectForAuthenticatedAdministrator(
                        5L,
                        ADMIN_EMAIL))
                .thenReturn(createResponse(
                        5L,
                        UnavailabilityStatus.REJECTED));

        mockMvc.perform(
                patch(
                        "/api/unavailabilities/5/rejection")
                        .principal(principal(ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("REJECTED"));
    }

    @Test
    void shouldReturnValidationErrors()
            throws Exception {
        mockMvc.perform(
                post("/api/unavailabilities")
                        .principal(principal(
                                FIREFIGHTER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": null,
                                  "startDate": null,
                                  "endDate": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.fieldErrors.type")
                                .exists())
                .andExpect(
                        jsonPath("$.fieldErrors.startDate")
                                .exists())
                .andExpect(
                        jsonPath("$.fieldErrors.endDate")
                                .exists());
    }

    @Test
    void shouldReturnBusinessRuleForInvalidPeriod()
            throws Exception {
        when(managementService
                .requestForAuthenticatedFirefighter(
                        any(),
                        any()))
                .thenThrow(
                        new InvalidUnavailabilityPeriodException());

        mockMvc.perform(
                post("/api/unavailabilities")
                        .principal(principal(
                                FIREFIGHTER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(
                        status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void shouldReturnNotFoundForMissingRequest()
            throws Exception {
        when(managementService
                .approveForAuthenticatedAdministrator(
                        99L,
                        ADMIN_EMAIL))
                .thenThrow(
                        new UnavailabilityNotFoundException(
                                99L));

        mockMvc.perform(
                patch(
                        "/api/unavailabilities/99/approval")
                        .principal(principal(ADMIN_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnConflictForReviewedRequest()
            throws Exception {
        when(managementService
                .rejectForAuthenticatedAdministrator(
                        6L,
                        ADMIN_EMAIL))
                .thenThrow(
                        new UnavailabilityAlreadyReviewedException(
                                6L));

        mockMvc.perform(
                patch(
                        "/api/unavailabilities/6/rejection")
                        .principal(principal(ADMIN_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private Principal principal(String email) {
        return () -> email;
    }

    private String validRequest() {
        return """
                {
                  "type": "PERSONAL_COMMITMENT",
                  "startDate": "2030-03-10",
                  "endDate": "2030-03-12",
                  "reason": "Compromisso familiar"
                }
                """;
    }

    private UnavailabilityResponse createResponse(
            Long id,
            UnavailabilityStatus status) {
        return new UnavailabilityResponse(
                id,
                20L,
                "Bombeiro Solicitante",
                UnavailabilityType.PERSONAL_COMMITMENT,
                LocalDate.of(2030, 3, 10),
                LocalDate.of(2030, 3, 12),
                status,
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0),
                status == UnavailabilityStatus.PENDING
                        ? null
                        : LocalDateTime.of(
                                2030,
                                2,
                                2,
                                10,
                                0));
    }
}