package br.com.escala24.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import br.com.escala24.service.MonthlyScheduleManagementService;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class PasswordChangeIntegrationTest {

    private static final String CURRENT_PASSWORD =
            "current-password";

    private static final String NEW_PASSWORD =
            "new-secure-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private MonthlyScheduleManagementService managementService;

    @Test
    void shouldRejectUnauthenticatedPasswordChange()
            throws Exception {
        mockMvc.perform(
                put("/api/users/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldBlockApiWhilePasswordChangeIsRequired()
            throws Exception {
        User user = createUser(
                "password-change-blocked@escala24.com",
                true
        );

        mockMvc.perform(
                get("/api/monthly-schedules/2029/1")
                        .with(httpBasic(
                                user.getEmail(),
                                CURRENT_PASSWORD
                        ))
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldChangePasswordAndRestoreRoleAccess()
            throws Exception {
        User user = createUser(
                "password-change-success@escala24.com",
                true
        );

        when(managementService.findByYearAndMonth(
                2029,
                1
        )).thenReturn(createScheduleResponse());

        mockMvc.perform(
                put("/api/users/me/password")
                        .with(csrf())
                        .with(httpBasic(
                                user.getEmail(),
                                CURRENT_PASSWORD
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
                .andExpect(status().isNoContent());

        userRepository.flush();

        User updatedUser = userRepository
                .findByEmail(user.getEmail())
                .orElseThrow();

        assertThat(updatedUser.isMustChangePassword())
                .isFalse();
        assertThat(passwordEncoder.matches(
                NEW_PASSWORD,
                updatedUser.getPassword()
        )).isTrue();
        assertThat(passwordEncoder.matches(
                CURRENT_PASSWORD,
                updatedUser.getPassword()
        )).isFalse();

        mockMvc.perform(
                get("/api/monthly-schedules/2029/1")
                        .with(httpBasic(
                                user.getEmail(),
                                NEW_PASSWORD
                        ))
        )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectIncorrectCurrentPassword()
            throws Exception {
        User user = createUser(
                "password-change-invalid-current@escala24.com",
                true
        );

        String previousHash = user.getPassword();

        mockMvc.perform(
                put("/api/users/me/password")
                        .with(csrf())
                        .with(httpBasic(
                                user.getEmail(),
                                CURRENT_PASSWORD
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "incorrect-password",
                                  "newPassword": "new-secure-password",
                                  "newPasswordConfirmation": "new-secure-password"
                                }
                                """)
        )
                .andExpect(
                        status().isUnprocessableContent()
                )
                .andExpect(jsonPath("$.status").value(422));

        assertPasswordWasNotChanged(
                user.getEmail(),
                previousHash
        );
    }

    @Test
    void shouldRejectPasswordConfirmationMismatch()
            throws Exception {
        User user = createUser(
                "password-change-mismatch@escala24.com",
                true
        );

        String previousHash = user.getPassword();

        mockMvc.perform(
                put("/api/users/me/password")
                        .with(csrf())
                        .with(httpBasic(
                                user.getEmail(),
                                CURRENT_PASSWORD
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "current-password",
                                  "newPassword": "new-secure-password",
                                  "newPasswordConfirmation": "different-password"
                                }
                                """)
        )
                .andExpect(
                        status().isUnprocessableContent()
                )
                .andExpect(jsonPath("$.status").value(422));

        assertPasswordWasNotChanged(
                user.getEmail(),
                previousHash
        );
    }

    @Test
    void shouldRejectCurrentPasswordReuse()
            throws Exception {
        User user = createUser(
                "password-change-reuse@escala24.com",
                true
        );

        String previousHash = user.getPassword();

        mockMvc.perform(
                put("/api/users/me/password")
                        .with(csrf())
                        .with(httpBasic(
                                user.getEmail(),
                                CURRENT_PASSWORD
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "current-password",
                                  "newPassword": "current-password",
                                  "newPasswordConfirmation": "current-password"
                                }
                                """)
        )
                .andExpect(
                        status().isUnprocessableContent()
                )
                .andExpect(jsonPath("$.status").value(422));

        assertPasswordWasNotChanged(
                user.getEmail(),
                previousHash
        );
    }

    private User createUser(
            String email,
            boolean mustChangePassword
    ) {
        User user = new User();
        user.setName("Usuário da Troca de Senha");
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(CURRENT_PASSWORD)
        );
        user.setRole(Role.FIREFIGHTER);
        user.setActive(true);
        user.setMustChangePassword(mustChangePassword);

        return userRepository.saveAndFlush(user);
    }

    private void assertPasswordWasNotChanged(
            String email,
            String previousHash
    ) {
        User unchangedUser = userRepository
                .findByEmail(email)
                .orElseThrow();

        assertThat(unchangedUser.getPassword())
                .isEqualTo(previousHash);
        assertThat(unchangedUser.isMustChangePassword())
                .isTrue();
    }

    private String validRequest() {
        return """
                {
                  "currentPassword": "current-password",
                  "newPassword": "new-secure-password",
                  "newPasswordConfirmation": "new-secure-password"
                }
                """;
    }

    private MonthlyScheduleGenerationResponse
            createScheduleResponse() {
        return new MonthlyScheduleGenerationResponse(
                200L,
                2029,
                1,
                ScheduleStatus.DRAFT,
                LocalDateTime.of(
                        2028,
                        12,
                        1,
                        10,
                        0
                ),
                List.of()
        );
    }
}
