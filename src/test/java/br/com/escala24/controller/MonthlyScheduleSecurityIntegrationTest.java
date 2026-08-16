package br.com.escala24.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.escala24.IntegrationTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.entity.User;
import br.com.escala24.repository.UserRepository;
import br.com.escala24.service.DutyReassignmentService;
import br.com.escala24.service.MonthlyScheduleGenerationService;
import br.com.escala24.service.MonthlyScheduleManagementService;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class MonthlyScheduleSecurityIntegrationTest {

    private static final String PASSWORD =
            "secure-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private MonthlyScheduleGenerationService generationService;

    @MockitoBean
    private MonthlyScheduleManagementService managementService;

    @MockitoBean
    private DutyReassignmentService reassignmentService;

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(
                get("/api/monthly-schedules/2028/8")
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(
                        jsonPath("$.error")
                                .value("Unauthorized")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/monthly-schedules/2028/8"
                                )
                );
    }

    @Test
    void shouldAllowFirefighterToFindSchedule()
            throws Exception {
        User firefighter = createUser(
                "security-firefighter@escala24.com",
                Role.FIREFIGHTER,
                true
        );

        when(managementService.findByYearAndMonth(2028, 8))
                .thenReturn(createScheduleResponse());

        mockMvc.perform(
                get("/api/monthly-schedules/2028/8")
                        .with(httpBasic(
                                firefighter.getEmail(),
                                PASSWORD
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2028))
                .andExpect(jsonPath("$.month").value(8));
    }

    @Test
    void shouldForbidFirefighterFromGeneratingSchedule()
            throws Exception {
        User firefighter = createUser(
                "security-restricted@escala24.com",
                Role.FIREFIGHTER,
                true
        );

        mockMvc.perform(
                post("/api/monthly-schedules")
                        .with(csrf())
                        .with(httpBasic(
                                firefighter.getEmail(),
                                PASSWORD
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2028,
                                  "month": 8
                                }
                                """)
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(
                        jsonPath("$.error")
                                .value("Forbidden")
                );
    }

    @Test
    void shouldAllowAdministratorToGenerateSchedule()
            throws Exception {
        User administrator = createUser(
                "security-admin@escala24.com",
                Role.ADMIN,
                true
        );

        when(generationService.generate(any()))
                .thenReturn(createScheduleResponse());

        mockMvc.perform(
                post("/api/monthly-schedules")
                        .with(csrf())
                        .with(httpBasic(
                                administrator.getEmail(),
                                PASSWORD
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2028,
                                  "month": 8
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldRejectInactiveUser() throws Exception {
        User inactiveUser = createUser(
                "security-inactive@escala24.com",
                Role.ADMIN,
                false
        );

        mockMvc.perform(
                get("/api/monthly-schedules/2028/8")
                        .with(httpBasic(
                                inactiveUser.getEmail(),
                                PASSWORD
                        ))
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private User createUser(
            String email,
            Role role,
            boolean active
    ) {
        User user = new User();
        user.setName("Usuário de Segurança");
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(PASSWORD)
        );
        user.setRole(role);
        user.setActive(active);
        user.setMustChangePassword(false);

        return userRepository.saveAndFlush(user);
    }

    private MonthlyScheduleGenerationResponse
            createScheduleResponse() {
        return new MonthlyScheduleGenerationResponse(
                100L,
                2028,
                8,
                ScheduleStatus.DRAFT,
                LocalDateTime.of(
                        2028,
                        7,
                        1,
                        10,
                        0
                ),
                List.of()
        );
    }
}
