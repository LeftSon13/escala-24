package br.com.escala24.exception;

public class HolidayNotFoundException extends RuntimeException {

    public HolidayNotFoundException(Long holidayId) {
        super("Feriado não encontrado com o ID: " + holidayId);
    }
}