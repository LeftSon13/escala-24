package br.com.escala24.exception;

import java.time.LocalDate;

public class HolidayAlreadyExistsException extends RuntimeException {

    public HolidayAlreadyExistsException(LocalDate date) {
        super("Já existe um feriado cadastrado para a data: " + date);
    }
}