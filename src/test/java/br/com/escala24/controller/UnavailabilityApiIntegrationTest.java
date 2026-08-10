package br.com.escala24.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.UnavailabilityRequest;
import br.com.escala24.dto.UnavailabilityResponse;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.Unavailability;
import br.com.escala24.entity.UnavailabilityStatus;
import br.com.escala24.entity.UnavailabilityType;
import br.com.escala24.entity.User;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.UnavailabilityRepository;
import br.com.escala24.repository.UserRepository;
import br.com.escala24.service.UnavailabilityManagementService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UnavailabilityApiIntegrationTest {

    private static final String PASSWORD =
            "unavailability-api-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirefighterRepository firefighterRepository;

    @Autowired
    private UnavailabilityRepository
            unavailabilityRepository;

    @Autowired
    private UnavailabilityManagementService
            managementService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldRejectUnauthenticatedRequest()
            throws Exception {
        mockMvc.perform(
                post("/api/unavailabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldForbidAdministratorFromRequesting()
            throws Exception {
        User administrator = createUser(
                "unavailability-api-admin-request@escala24.com",
                Role.ADMIN,
                false
        );

        mockMvc.perform(
                post("/api/unavailabilities")
                        .with(httpBasic(
                                administrator.getEmail(),
                                PASSWORD
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldForbidPendingPasswordUser()
            throws Exception {
        Firefighter firefighter = createFirefighter(
                1,
                true
        );

        mockMvc.perform(
                post("/api/unavailabilities")
                        .with(httpBasic(
                                firefighter.getUser().getEmail(),
                                PASSWORD
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldCreateAndListOnlyOwnRequests()
            throws Exception {
        Firefighter firstFirefighter =
                createFirefighter(2, false);

        Firefighter secondFirefighter =
                createFirefighter(3, false);

        requestByHttp(firstFirefighter);
        requestByHttp(secondFirefighter);

        mockMvc.perform(
                get("/api/unavailabilities/me")
                        .with(httpBasic(
                                firstFirefighter
                                        .getUser()
                                        .getEmail(),
                                PASSWORD
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].firefighterId")
                                .value(
                                        firstFirefighter.getId()
                                )
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("PENDING")
                );
    }

    @Test
    void shouldForbidFirefighterFromListingPending()
            throws Exception {
        Firefighter firefighter =
                createFirefighter(4, false);

        mockMvc.perform(
                get("/api/unavailabilities/pending")
                        .with(httpBasic(
                                firefighter.getUser().getEmail(),
                                PASSWORD
                        ))
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldAllowAdministratorToListAndApprove()
            throws Exception {
        Firefighter firefighter =
                createFirefighter(5, false);

        User administrator = createUser(
                "unavailability-api-review-admin@escala24.com",
                Role.ADMIN,
                false
        );

        UnavailabilityResponse requested =
                managementService.request(
                        firefighter.getId(),
                        new UnavailabilityRequest(
                                UnavailabilityType
                                        .PERSONAL_COMMITMENT,
                                LocalDate.of(2031, 4, 10),
                                LocalDate.of(2031, 4, 12),
                                "Compromisso familiar"
                        )
                );

        mockMvc.perform(
                get("/api/unavailabilities/pending")
                        .with(httpBasic(
                                administrator.getEmail(),
                                PASSWORD
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].id")
                                .value(requested.id())
                );

        mockMvc.perform(
                patch(
                        "/api/unavailabilities/"
                                + requested.id()
                                + "/approval"
                )
                        .with(httpBasic(
                                administrator.getEmail(),
                                PASSWORD
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("APPROVED")
                );

        unavailabilityRepository.flush();

        Unavailability approved =
                unavailabilityRepository
                        .findById(requested.id())
                        .orElseThrow();

        assertThat(approved.getStatus())
                .isEqualTo(
                        UnavailabilityStatus.APPROVED
                );
        assertThat(approved.getReviewedBy().getId())
                .isEqualTo(administrator.getId());
        assertThat(approved.getReviewedAt()).isNotNull();
    }

    private void requestByHttp(
            Firefighter firefighter
    ) throws Exception {
        mockMvc.perform(
                post("/api/unavailabilities")
                        .with(httpBasic(
                                firefighter.getUser().getEmail(),
                                PASSWORD
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest())
        )
                .andExpect(status().isCreated());
    }

    private Firefighter createFirefighter(
            int number,
            boolean mustChangePassword
    ) {
        User user = createUser(
                "unavailability-api-firefighter-"
                        + number
                        + "@escala24.com",
                Role.FIREFIGHTER,
                mustChangePassword
        );

        Firefighter firefighter = new Firefighter();
        firefighter.setUser(user);
        firefighter.setRegistration(
                "REG-UNAVAILABILITY-API-" + number
        );
        firefighter.setPhone(
                "1177777777" + number
        );

        return firefighterRepository
                .saveAndFlush(firefighter);
    }

    private User createUser(
            String email,
            Role role,
            boolean mustChangePassword
    ) {
        User user = new User();
        user.setName("Usuário da API de Indisponibilidade");
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

    private String validRequest() {
        return """
                {
                  "type": "PERSONAL_COMMITMENT",
                  "startDate": "2031-04-10",
                  "endDate": "2031-04-12",
                  "reason": "Compromisso familiar"
                }
                """;
    }
}