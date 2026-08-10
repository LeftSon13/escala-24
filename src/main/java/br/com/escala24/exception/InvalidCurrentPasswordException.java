package br.com.escala24.exception;

public class InvalidCurrentPasswordException
        extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("A senha atual informada está incorreta");
    }
}