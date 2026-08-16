package br.com.escala24.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import br.com.escala24.dto.HolidayResponse;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;
import br.com.escala24.repository.UserRepository;
import br.com.escala24.service.HolidayManagementService;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class HolidaySecurityIntegrationTest {

    private static final String PASSWORD =
            "holiday-security-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private HolidayManagementService holidayService;

    @Test
    void shouldRejectUnauthenticatedAccess()
            throws Exception {
        mockMvc.perform(
                get("/api/holidays")
                        .param("year", "2096")
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(
                        jsonPath("$.error")
                                .value("Unauthorized")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/holidays")
                );
    }

    @Test
    void shouldAllowFirefighterToListHolidays()
            throws Exception {
        User firefighter = createUser(
                "holiday-security-firefighter@escala24.com",
                Role.FIREFIGHTER,
                true,
                false
        );

        when(holidayService.listByYear(2096))
                .thenReturn(
                        List.of(
                                new HolidayResponse(
                                        1L,
                                        LocalDate.of(
                                                2096,
                                                9,
                                                7
                                        ),
                                        "Independência do Brasil",
                                        LocalDateTime.of(
                                                2096,
                                                1,
                                                10,
                                                10,
                                                0
                                        )
                                )
                        )
                );

        mockMvc.perform(
                get("/api/holidays")
                        .param("year", "2096")
                        .with(
                                httpBasic(
                                        firefighter.getEmail(),
                                        PASSWORD
                                )
                        )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].name")
                                .value(
                                        "Independência do Brasil"
                                )
                );

        verify(holidayService).listByYear(2096);
    }

    @Test
    void shouldForbidFirefighterFromCreatingHoliday()
            throws Exception {
        User firefighter = createUser(
                "holiday-security-restricted@escala24.com",
                Role.FIREFIGHTER,
                true,
                false
        );

        mockMvc.perform(
                post("/api/holidays")
                        .with(
                                httpBasic(
                                        firefighter.getEmail(),
                                        PASSWORD
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2096-09-07",
                                  "name": "Independência do Brasil"
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
    void shouldAllowAdministratorToCreateHoliday()
            throws Exception {
        User administrator = createUser(
                "holiday-security-admin@escala24.com",
                Role.ADMIN,
                true,
                false
        );

        when(holidayService.create(any()))
                .thenReturn(
                        new HolidayResponse(
                                2L,
                                LocalDate.of(
                                        2096,
                                        11,
                                        15
                                ),
                                "Proclamação da República",
                                LocalDateTime.of(
                                        2096,
                                        1,
                                        10,
                                        11,
                                        0
                                )
                        )
                );

        mockMvc.perform(
                post("/api/holidays")
                        .with(
                                httpBasic(
                                        administrator.getEmail(),
                                        PASSWORD
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2096-11-15",
                                  "name": "Proclamação da República"
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Proclamação da República"
                                )
                );
    }

    @Test
    void shouldAllowAdministratorToDeleteHoliday()
            throws Exception {
        User administrator = createUser(
                "holiday-security-delete@escala24.com",
                Role.ADMIN,
                true,
                false
        );

        mockMvc.perform(
                delete("/api/holidays/10")
                        .with(
                                httpBasic(
                                        administrator.getEmail(),
                                        PASSWORD
                                )
                        )
        )
                .andExpect(status().isNoContent());

        verify(holidayService).delete(10L);
    }

    @Test
    void shouldForbidUserWithPendingPasswordChange()
            throws Exception {
        User administrator = createUser(
                "holiday-security-pending@escala24.com",
                Role.ADMIN,
                true,
                true
        );

        mockMvc.perform(
                get("/api/holidays")
                        .param("year", "2096")
                        .with(
                                httpBasic(
                                        administrator.getEmail(),
                                        PASSWORD
                                )
                        )
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldRejectInactiveUser()
            throws Exception {
        User inactiveAdministrator = createUser(
                "holiday-security-inactive@escala24.com",
                Role.ADMIN,
                false,
                false
        );

        mockMvc.perform(
                get("/api/holidays")
                        .param("year", "2096")
                        .with(
                                httpBasic(
                                        inactiveAdministrator.getEmail(),
                                        PASSWORD
                                )
                        )
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private User createUser(
            String email,
            Role role,
            boolean active,
            boolean mustChangePassword
    ) {
        User user = new User();
        user.setName("Usuário da API de Feriados");
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(PASSWORD)
        );
        user.setRole(role);
        user.setActive(active);
        user.setMustChangePassword(
                mustChangePassword
        );

        return userRepository.saveAndFlush(user);
    }
}
