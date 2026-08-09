package br.com.escala24.exception;

public class DutyReassignmentRequiredException
        extends RuntimeException {

    public DutyReassignmentRequiredException() {
        super(
                "O bombeiro possui plantão no período; "
                        + "é necessário definir um substituto antes da aprovação"
        );
    }
}