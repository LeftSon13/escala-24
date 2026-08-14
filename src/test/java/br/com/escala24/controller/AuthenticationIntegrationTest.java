package br.com.escala24.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;
import br.com.escala24.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationIntegrationTest {

  private static final String PASSWORD = "secure-password";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void shouldAuthenticateAdministratorAndCreateSession()
      throws Exception {
    createUser(
        "Administrador",
        "admin-login@escala24.com",
        Role.ADMIN);

    mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "admin-login@escala24.com",
                  "password": "secure-password"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name")
            .value("Administrador"))
        .andExpect(jsonPath("$.email")
            .value("admin-login@escala24.com"))
        .andExpect(jsonPath("$.role")
            .value("ADMIN"))
        .andExpect(jsonPath("$.mustChangePassword")
            .value(false))
        .andExpect(request().sessionAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            notNullValue()));
  }

  @Test
  void shouldAuthenticateFirefighterAndCreateSession()
      throws Exception {
    createUser(
        "Bombeiro",
        "firefighter-login@escala24.com",
        Role.FIREFIGHTER);

    mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "firefighter-login@escala24.com",
                  "password": "secure-password"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name")
            .value("Bombeiro"))
        .andExpect(jsonPath("$.email")
            .value("firefighter-login@escala24.com"))
        .andExpect(jsonPath("$.role")
            .value("FIREFIGHTER"))
        .andExpect(jsonPath("$.mustChangePassword")
            .value(false))
        .andExpect(request().sessionAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            notNullValue()));
  }

  @Test
  void shouldRejectInvalidCredentialsWithoutCreatingSession()
      throws Exception {
    createUser(
        "Administrador",
        "invalid-login@escala24.com",
        Role.ADMIN);

    mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "invalid-login@escala24.com",
                  "password": "wrong-password"
                }
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status")
            .value(401))
        .andExpect(jsonPath("$.message")
            .value("Credenciais inválidas"))
        .andExpect(jsonPath("$.path")
            .value("/api/auth/login"))
        .andExpect(result -> assertThat(
            result.getRequest()
                .getSession(false))
            .isNull());
  }

  @Test
  void shouldReturnAuthenticatedUserFromSession()
      throws Exception {
    createUser(
        "Administrador da sessão",
        "session-user@escala24.com",
        Role.ADMIN);

    MvcResult loginResult = mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "session-user@escala24.com",
                  "password": "secure-password"
                }
                """))
        .andExpect(status().isOk())
        .andReturn();

    MockHttpSession session = (MockHttpSession) loginResult
        .getRequest()
        .getSession(false);

    assertThat(session).isNotNull();

    mockMvc.perform(
        get("/api/users/me")
            .session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name")
            .value("Administrador da sessão"))
        .andExpect(jsonPath("$.email")
            .value("session-user@escala24.com"))
        .andExpect(jsonPath("$.role")
            .value("ADMIN"))
        .andExpect(jsonPath("$.mustChangePassword")
            .value(false));
  }

  @Test
  void shouldReturnUserThatMustChangePasswordFromSession()
      throws Exception {
    User user = createUser(
        "Usuário com senha temporária",
        "password-change-session@escala24.com",
        Role.FIREFIGHTER);

    user.setMustChangePassword(true);
    userRepository.saveAndFlush(user);

    MvcResult loginResult = mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "password-change-session@escala24.com",
                  "password": "secure-password"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mustChangePassword")
            .value(true))
        .andReturn();

    MockHttpSession session = (MockHttpSession) loginResult
        .getRequest()
        .getSession(false);

    assertThat(session).isNotNull();

    mockMvc.perform(
        get("/api/users/me")
            .session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name")
            .value("Usuário com senha temporária"))
        .andExpect(jsonPath("$.email")
            .value("password-change-session@escala24.com"))
        .andExpect(jsonPath("$.role")
            .value("FIREFIGHTER"))
        .andExpect(jsonPath("$.mustChangePassword")
            .value(true));
  }

  @Test
  void shouldRejectAnonymousCurrentUserRequest()
      throws Exception {
    mockMvc.perform(
        get("/api/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status")
            .value(401))
        .andExpect(jsonPath("$.message")
            .value(
                "Autenticação necessária para acessar este recurso"))
        .andExpect(jsonPath("$.path")
            .value("/api/users/me"))
        .andExpect(result -> assertThat(
            result.getRequest()
                .getSession(false))
            .isNull());
  }

  @Test
  void shouldInvalidateSessionOnLogout()
      throws Exception {
    createUser(
        "Administrador para logout",
        "logout-session@escala24.com",
        Role.ADMIN);

    MvcResult loginResult = mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "logout-session@escala24.com",
                  "password": "secure-password"
                }
                """))
        .andExpect(status().isOk())
        .andReturn();

    MockHttpSession session = (MockHttpSession) loginResult
        .getRequest()
        .getSession(false);

    assertThat(session).isNotNull();
    assertThat(session.isInvalid()).isFalse();

    mockMvc.perform(
        get("/api/users/me")
            .session(session))
        .andExpect(status().isOk());

    mockMvc.perform(
        post("/api/auth/logout")
            .session(session))
        .andExpect(status().isNoContent());

    assertThat(session.isInvalid()).isTrue();

    mockMvc.perform(
        get("/api/users/me"))
        .andExpect(status().isUnauthorized());
  }

  private User createUser(
      String name,
      String email,
      Role role) {
    User user = new User();
    user.setName(name);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(PASSWORD));
    user.setRole(role);
    user.setActive(true);
    user.setMustChangePassword(false);

    return userRepository.saveAndFlush(user);
  }
}