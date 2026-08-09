package br.com.escala24.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class MonthlyScheduleGenerationRequestTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void shouldAcceptValidYearAndMonth() {
        MonthlyScheduleGenerationRequest request =
                new MonthlyScheduleGenerationRequest(
                        2027,
                        8
                );

        Set<ConstraintViolation<MonthlyScheduleGenerationRequest>>
                violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectNonPositiveYearAndMonthAboveRange() {
        MonthlyScheduleGenerationRequest request =
                new MonthlyScheduleGenerationRequest(
                        0,
                        13
                );

        Set<ConstraintViolation<MonthlyScheduleGenerationRequest>>
                violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .containsExactlyInAnyOrder(
                        "year",
                        "month"
                );
    }

    @Test
    void shouldRejectMonthBelowRange() {
        MonthlyScheduleGenerationRequest request =
                new MonthlyScheduleGenerationRequest(
                        2027,
                        0
                );

        Set<ConstraintViolation<MonthlyScheduleGenerationRequest>>
                violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .containsExactly("month");
    }
}