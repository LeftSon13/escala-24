package br.com.escala24.exception;

public class PasswordConfirmationMismatchException
        extends RuntimeException {

    public PasswordConfirmationMismatchException() {
        super("A confirmação da nova senha não corresponde à nova senha");
    }
}