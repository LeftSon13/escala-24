package br.com.escala24.exception;

public class InvalidUnavailabilityPeriodException
        extends RuntimeException {

    public InvalidUnavailabilityPeriodException() {
        super("A data final não pode ser anterior à data inicial");
    }
}