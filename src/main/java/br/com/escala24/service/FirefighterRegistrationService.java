package br.com.escala24.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import br.com.escala24.dto.FirefighterRegistrationRequest;
import br.com.escala24.dto.FirefighterRegistrationResponse;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;
import br.com.escala24.exception.EmailAlreadyExistsException;
import br.com.escala24.exception.RegistrationAlreadyExistsException;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.UserRepository;
import jakarta.validation.Valid;

@Service
@Validated
public class FirefighterRegistrationService {

    private final UserRepository userRepository;
    private final FirefighterRepository firefighterRepository;
    private final PasswordEncoder passwordEncoder;

    public FirefighterRegistrationService(
            UserRepository userRepository,
            FirefighterRepository firefighterRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.firefighterRepository = firefighterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public FirefighterRegistrationResponse register(
            @Valid FirefighterRegistrationRequest request) {
        String normalizedName = request.name().trim();
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);
        String normalizedRegistration = request.registration()
                .trim()
                .toUpperCase(Locale.ROOT);
        String normalizedPhone = request.phone().trim();

        validateUniqueEmail(normalizedEmail);
        validateUniqueRegistration(normalizedRegistration);

        User user = new User();
        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPassword(
                passwordEncoder.encode(request.temporaryPassword())
        );
        user.setRole(Role.FIREFIGHTER);
        user.setActive(true);
        user.setMustChangePassword(true);

        User savedUser = userRepository.save(user);

        Firefighter firefighter = new Firefighter();
        firefighter.setUser(savedUser);
        firefighter.setRegistration(normalizedRegistration);
        firefighter.setPhone(normalizedPhone);

        Firefighter savedFirefighter =
                firefighterRepository.save(firefighter);

        return new FirefighterRegistrationResponse(
                savedFirefighter.getId(),
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedFirefighter.getRegistration(),
                savedFirefighter.getPhone(),
                savedUser.isActive(),
                savedUser.isMustChangePassword()
        );
    }

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    private void validateUniqueRegistration(String registration) {
        if (firefighterRepository.existsByRegistration(registration)) {
            throw new RegistrationAlreadyExistsException(registration);
        }
    }
}