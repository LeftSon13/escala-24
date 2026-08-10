package br.com.escala24.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class DutyReassignmentRequestTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void shouldAcceptPositiveFirefighterId() {
        DutyReassignmentRequest request =
                new DutyReassignmentRequest(1L);

        Set<ConstraintViolation<DutyReassignmentRequest>>
                violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectNullFirefighterId() {
        DutyReassignmentRequest request =
                new DutyReassignmentRequest(null);

        Set<ConstraintViolation<DutyReassignmentRequest>>
                violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .containsExactly("firefighterId");
    }

    @Test
    void shouldRejectNonPositiveFirefighterId() {
        DutyReassignmentRequest request =
                new DutyReassignmentRequest(0L);

        Set<ConstraintViolation<DutyReassignmentRequest>>
                violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .containsExactly("firefighterId");
    }
}