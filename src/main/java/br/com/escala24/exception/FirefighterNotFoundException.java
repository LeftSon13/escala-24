package br.com.escala24.exception;

public class FirefighterNotFoundException extends RuntimeException {

    public FirefighterNotFoundException(Long firefighterId) {
        super("Bombeiro não encontrado com o ID: " + firefighterId);
    }
}