package br.com.escala24.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;

@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        User user = createUser("admin@escala24.com");

        User savedUser = userRepository.saveAndFlush(user);
        Optional<User> foundUser = userRepository.findByEmail("admin@escala24.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldIdentifyExistingEmail() {
        User user = createUser("existing@escala24.com");

        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByEmail("existing@escala24.com"))
                .isTrue();
        assertThat(userRepository.existsByEmail("unknown@escala24.com"))
                .isFalse();
    }

    private User createUser(String email) {
        User user = new User();
        user.setName("Administrador de Teste");
        user.setEmail(email);
        user.setPassword("hashed-password-for-test");
        user.setRole(Role.ADMIN);
        user.setActive(true);
        return user;
    }
}