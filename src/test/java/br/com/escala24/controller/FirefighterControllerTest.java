package br.com.escala24.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.escala24.dto.FirefighterRegistrationRequest;
import br.com.escala24.dto.FirefighterRegistrationResponse;
import br.com.escala24.dto.FirefighterSummaryResponse;
import br.com.escala24.exception.EmailAlreadyExistsException;
import br.com.escala24.exception.FirefighterNotFoundException;
import br.com.escala24.service.FirefighterManagementService;
import br.com.escala24.service.FirefighterRegistrationService;

@WebMvcTest(FirefighterController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FirefighterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FirefighterRegistrationService
            registrationService;

    @MockitoBean
    private FirefighterManagementService
            managementService;

    @Test
    void shouldRegisterFirefighter() throws Exception {
        FirefighterRegistrationResponse response =
                new FirefighterRegistrationResponse(
                        1L,
                        2L,
                        "Bombeiro da API",
                        "firefighter-api@escala24.com",
                        "REG-API-1",
                        "11999999999",
                        true,
                        true
                );

        when(registrationService.register(any()))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/firefighters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bombeiro da API",
                                  "email": "firefighter-api@escala24.com",
                                  "temporaryPassword": "temporary-password",
                                  "registration": "REG-API-1",
                                  "phone": "11999999999"
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.firefighterId").value(1)
                )
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(
                        jsonPath("$.mustChangePassword")
                                .value(true)
                );

        verify(registrationService).register(
                new FirefighterRegistrationRequest(
                        "Bombeiro da API",
                        "firefighter-api@escala24.com",
                        "temporary-password",
                        "REG-API-1",
                        "11999999999"
                )
        );
    }

    @Test
    void shouldListFirefighters() throws Exception {
        when(managementService.listAll())
                .thenReturn(List.of(
                        createSummary(1L, true),
                        createSummary(2L, false)
                ));

        mockMvc.perform(get("/api/firefighters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(
                        jsonPath("$[0].firefighterId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[1].active")
                                .value(false)
                );

        verify(managementService).listAll();
    }

    @Test
    void shouldDeactivateFirefighter() throws Exception {
        when(managementService.deactivate(1L))
                .thenReturn(createSummary(1L, false));

        mockMvc.perform(
                patch(
                        "/api/firefighters/1/deactivation"
                )
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.firefighterId").value(1)
                )
                .andExpect(
                        jsonPath("$.active").value(false)
                );

        verify(managementService).deactivate(1L);
    }

    @Test
    void shouldReturnValidationErrors()
            throws Exception {
        mockMvc.perform(
                post("/api/firefighters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "temporaryPassword": "short",
                                  "registration": "",
                                  "phone": ""
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.fieldErrors.name")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.fieldErrors.email")
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.fieldErrors.temporaryPassword"
                        ).exists()
                )
                .andExpect(
                        jsonPath("$.fieldErrors.registration")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.fieldErrors.phone")
                                .exists()
                );
    }

    @Test
    void shouldReturnConflictForExistingEmail()
            throws Exception {
        String email = "existing@escala24.com";

        when(registrationService.register(any()))
                .thenThrow(
                        new EmailAlreadyExistsException(email)
                );

        mockMvc.perform(
                post("/api/firefighters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bombeiro Existente",
                                  "email": "existing@escala24.com",
                                  "temporaryPassword": "temporary-password",
                                  "registration": "REG-API-2",
                                  "phone": "11888888888"
                                }
                                """)
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Já existe um usuário cadastrado com o e-mail: "
                                                + email
                                )
                );
    }

    @Test
    void shouldReturnNotFoundWhenDeactivating()
            throws Exception {
        when(managementService.deactivate(99L))
                .thenThrow(
                        new FirefighterNotFoundException(99L)
                );

        mockMvc.perform(
                patch(
                        "/api/firefighters/99/deactivation"
                )
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/firefighters/99/deactivation"
                                )
                );
    }

    private FirefighterSummaryResponse createSummary(
            Long firefighterId,
            boolean active
    ) {
        return new FirefighterSummaryResponse(
                firefighterId,
                firefighterId + 10,
                "Bombeiro " + firefighterId,
                "firefighter-"
                        + firefighterId
                        + "@escala24.com",
                "REG-" + firefighterId,
                "11999999999",
                active
        );
    }
}