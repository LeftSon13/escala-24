package br.com.escala24.exception;

public class UnavailabilityAlreadyReviewedException
        extends RuntimeException {

    public UnavailabilityAlreadyReviewedException(Long id) {
        super("A indisponibilidade já foi analisada: " + id);
    }
}