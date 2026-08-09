package br.com.escala24.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;

@SpringBootTest
@Transactional
class FirefighterRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirefighterRepository firefighterRepository;

    @Test
    void shouldFindFirefighterByRegistration() {
        User user = userRepository.saveAndFlush(
                createUser("firefighter@escala24.com")
        );

        Firefighter firefighter = createFirefighter(user, "REG-001");
        Firefighter savedFirefighter =
                firefighterRepository.saveAndFlush(firefighter);

        Optional<Firefighter> foundFirefighter =
                firefighterRepository.findByRegistration("REG-001");

        assertThat(foundFirefighter).isPresent();
        assertThat(foundFirefighter.get().getId())
                .isEqualTo(savedFirefighter.getId());
        assertThat(foundFirefighter.get().getUser().getId())
                .isEqualTo(user.getId());
    }

    @Test
    void shouldIdentifyExistingRegistration() {
        User user = userRepository.saveAndFlush(
                createUser("existing-firefighter@escala24.com")
        );

        firefighterRepository.saveAndFlush(
                createFirefighter(user, "REG-002")
        );

        assertThat(firefighterRepository.existsByRegistration("REG-002"))
                .isTrue();
        assertThat(firefighterRepository.existsByRegistration("REG-999"))
                .isFalse();
    }

    private User createUser(String email) {
        User user = new User();
        user.setName("Bombeiro de Teste");
        user.setEmail(email);
        user.setPassword("hashed-password-for-test");
        user.setRole(Role.FIREFIGHTER);
        user.setActive(true);
        return user;
    }

    private Firefighter createFirefighter(User user, String registration) {
        Firefighter firefighter = new Firefighter();
        firefighter.setUser(user);
        firefighter.setRegistration(registration);
        firefighter.setPhone("11999999999");
        return firefighter;
    }
}