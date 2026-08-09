package br.com.escala24.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class FirefighterRegistrationRequestTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void shouldAcceptValidRegistrationData() {
        FirefighterRegistrationRequest request =
                new FirefighterRegistrationRequest(
                        "João da Silva",
                        "joao@escala24.com",
                        "temporary-password",
                        "REG-001",
                        "11999999999"
                );

        Set<ConstraintViolation<FirefighterRegistrationRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectInvalidRegistrationData() {
        FirefighterRegistrationRequest request =
                new FirefighterRegistrationRequest(
                        "",
                        "invalid-email",
                        "short",
                        "",
                        ""
                );

        Set<ConstraintViolation<FirefighterRegistrationRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains(
                        "name",
                        "email",
                        "temporaryPassword",
                        "registration",
                        "phone"
                );
    }
}