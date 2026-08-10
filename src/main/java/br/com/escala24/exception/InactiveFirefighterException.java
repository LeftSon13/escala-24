package br.com.escala24.exception;

public class InactiveFirefighterException
        extends RuntimeException {

    public InactiveFirefighterException(Long firefighterId) {
        super(
                "O bombeiro "
                        + firefighterId
                        + " está inativo e não pode receber plantões"
        );
    }
}