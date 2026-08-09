package br.com.escala24.exception;

public class RegistrationAlreadyExistsException extends RuntimeException {

    public RegistrationAlreadyExistsException(String registration) {
        super("Já existe um bombeiro cadastrado com a matrícula: "
                + registration);
    }
}