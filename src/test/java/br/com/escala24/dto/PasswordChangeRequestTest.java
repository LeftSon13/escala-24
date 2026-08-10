package br.com.escala24.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PasswordChangeRequestTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void shouldAcceptValidPasswordChangeRequest() {
        PasswordChangeRequest request =
                new PasswordChangeRequest(
                        "current-password",
                        "new-secure-password",
                        "new-secure-password"
                );

        Set<ConstraintViolation<PasswordChangeRequest>>
                violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectBlankCurrentPassword() {
        PasswordChangeRequest request =
                new PasswordChangeRequest(
                        " ",
                        "new-secure-password",
                        "new-secure-password"
                );

        Set<ConstraintViolation<PasswordChangeRequest>>
                violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .contains("currentPassword");
    }

    @Test
    void shouldRejectShortNewPassword() {
        PasswordChangeRequest request =
                new PasswordChangeRequest(
                        "current-password",
                        "short",
                        "short"
                );

        Set<ConstraintViolation<PasswordChangeRequest>>
                violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .contains(
                        "newPassword",
                        "newPasswordConfirmation"
                );
    }

    @Test
    void shouldRejectBlankPasswordConfirmation() {
        PasswordChangeRequest request =
                new PasswordChangeRequest(
                        "current-password",
                        "new-secure-password",
                        " "
                );

        Set<ConstraintViolation<PasswordChangeRequest>>
                violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .contains("newPasswordConfirmation");
    }
}