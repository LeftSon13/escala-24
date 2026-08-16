package br.com.escala24.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import br.com.escala24.dto.FirefighterRegistrationResponse;
import br.com.escala24.dto.FirefighterSummaryResponse;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;
import br.com.escala24.repository.UserRepository;
import br.com.escala24.service.FirefighterManagementService;
import br.com.escala24.service.FirefighterRegistrationService;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class FirefighterSecurityIntegrationTest {

    private static final String PASSWORD =
            "firefighter-api-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private FirefighterRegistrationService
            registrationService;

    @MockitoBean
    private FirefighterManagementService
            managementService;

    @Test
    void shouldRejectUnauthenticatedAccess()
            throws Exception {
        mockMvc.perform(get("/api/firefighters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldForbidFirefighterAccess()
            throws Exception {
        User firefighter = createUser(
                "firefighter-api-role@escala24.com",
                Role.FIREFIGHTER,
                false
        );

        mockMvc.perform(
                get("/api/firefighters")
                        .with(httpBasic(
                                firefighter.getEmail(),
                                PASSWORD
                        ))
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldForbidAdministratorWithPendingPasswordChange()
            throws Exception {
        User administrator = createUser(
                "firefighter-api-pending@escala24.com",
                Role.ADMIN,
                true
        );

        mockMvc.perform(
                get("/api/firefighters")
                        .with(httpBasic(
                                administrator.getEmail(),
                                PASSWORD
                        ))
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldAllowAdministratorToListFirefighters()
            throws Exception {
        User administrator = createUser(
                "firefighter-api-list-admin@escala24.com",
                Role.ADMIN,
                false
        );

        when(managementService.listAll())
                .thenReturn(List.of(
                        new FirefighterSummaryResponse(
                                1L,
                                2L,
                                "Bombeiro da Segurança",
                                "listed@escala24.com",
                                "REG-LISTED",
                                "11999999999",
                                true
                        )
                ));

        mockMvc.perform(
                get("/api/firefighters")
                        .with(httpBasic(
                                administrator.getEmail(),
                                PASSWORD
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldAllowAdministratorToRegisterFirefighter()
            throws Exception {
        User administrator = createUser(
                "firefighter-api-register-admin@escala24.com",
                Role.ADMIN,
                false
        );

        when(registrationService.register(any()))
                .thenReturn(
                        new FirefighterRegistrationResponse(
                                10L,
                                20L,
                                "Novo Bombeiro",
                                "new-firefighter@escala24.com",
                                "REG-NEW",
                                "11888888888",
                                true,
                                true
                        )
                );

        mockMvc.perform(
                post("/api/firefighters")
                        .with(httpBasic(
                                administrator.getEmail(),
                                PASSWORD
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Novo Bombeiro",
                                  "email": "new-firefighter@escala24.com",
                                  "temporaryPassword": "temporary-password",
                                  "registration": "REG-NEW",
                                  "phone": "11888888888"
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.firefighterId")
                                .value(10)
                );
    }

    private User createUser(
            String email,
            Role role,
            boolean mustChangePassword
    ) {
        User user = new User();
        user.setName("Usuário da API de Bombeiros");
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(PASSWORD)
        );
        user.setRole(role);
        user.setActive(true);
        user.setMustChangePassword(
                mustChangePassword
        );

        return userRepository.saveAndFlush(user);
    }
}
