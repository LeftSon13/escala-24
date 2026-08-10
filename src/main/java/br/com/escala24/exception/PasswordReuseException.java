package br.com.escala24.exception;

public class PasswordReuseException
        extends RuntimeException {

    public PasswordReuseException() {
        super("A nova senha deve ser diferente da senha atual");
    }
}