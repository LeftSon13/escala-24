package br.com.escala24.exception;

public class UnavailabilityNotFoundException
        extends RuntimeException {

    public UnavailabilityNotFoundException(Long id) {
        super("Indisponibilidade não encontrada: " + id);
    }
}