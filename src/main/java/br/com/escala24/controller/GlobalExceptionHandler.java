package br.com.escala24.controller;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.escala24.dto.ApiErrorResponse;
import br.com.escala24.exception.DutyAssignmentNotFoundException;
import br.com.escala24.exception.FirefighterNotFoundException;
import br.com.escala24.exception.FirefighterUnavailableForDutyException;
import br.com.escala24.exception.InactiveFirefighterException;
import br.com.escala24.exception.IncompleteMonthlyScheduleException;
import br.com.escala24.exception.MandatoryRestViolationException;
import br.com.escala24.exception.MonthlyScheduleAlreadyExistsException;
import br.com.escala24.exception.MonthlyScheduleAlreadyPublishedException;
import br.com.escala24.exception.MonthlyScheduleNotFoundException;
import br.com.escala24.exception.NoEligibleFirefighterException;
import br.com.escala24.exception.PublishedScheduleModificationException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler({
                        MonthlyScheduleNotFoundException.class,
                        DutyAssignmentNotFoundException.class,
                        FirefighterNotFoundException.class
        })
        public ResponseEntity<ApiErrorResponse> handleNotFound(
                        RuntimeException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.NOT_FOUND,
                                exception.getMessage(),
                                request,
                                Map.of());
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
                        HttpMessageNotReadableException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "A requisição possui formato inválido",
                                request,
                                Map.of());
        }

        @ExceptionHandler({
                        MonthlyScheduleAlreadyExistsException.class,
                        MonthlyScheduleAlreadyPublishedException.class,
                        PublishedScheduleModificationException.class
        })
        public ResponseEntity<ApiErrorResponse> handleConflict(
                        RuntimeException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.CONFLICT,
                                exception.getMessage(),
                                request,
                                Map.of());
        }

        @ExceptionHandler({
                        FirefighterUnavailableForDutyException.class,
                        InactiveFirefighterException.class,
                        MandatoryRestViolationException.class,
                        IncompleteMonthlyScheduleException.class,
                        NoEligibleFirefighterException.class
        })
        public ResponseEntity<ApiErrorResponse> handleBusinessRule(
                        RuntimeException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.UNPROCESSABLE_CONTENT,
                                exception.getMessage(),
                                request,
                                Map.of());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleValidation(
                        MethodArgumentNotValidException exception,
                        HttpServletRequest request) {
                Map<String, String> fieldErrors = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(
                                                Collectors.toMap(
                                                                fieldError -> fieldError.getField(),
                                                                fieldError -> {
                                                                        String message = fieldError
                                                                                        .getDefaultMessage();

                                                                        return message != null
                                                                                        ? message
                                                                                        : "Valor inválido";
                                                                },
                                                                (firstMessage, ignored) -> firstMessage));

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "A requisição possui campos inválidos",
                                request,
                                fieldErrors);
        }

        @ExceptionHandler({
                        IllegalArgumentException.class,
                        DateTimeException.class
        })
        public ResponseEntity<ApiErrorResponse> handleInvalidArgument(
                        RuntimeException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                exception.getMessage(),
                                request,
                                Map.of());
        }

        private ResponseEntity<ApiErrorResponse> buildResponse(
                        HttpStatus status,
                        String message,
                        HttpServletRequest request,
                        Map<String, String> fieldErrors) {
                ApiErrorResponse response = new ApiErrorResponse(
                                LocalDateTime.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                message,
                                request.getRequestURI(),
                                fieldErrors);

                return ResponseEntity
                                .status(status)
                                .body(response);
        }
}