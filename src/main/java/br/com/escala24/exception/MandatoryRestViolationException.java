package br.com.escala24.exception;

import java.time.LocalDate;

public class MandatoryRestViolationException
        extends RuntimeException {

    public MandatoryRestViolationException(
            Long firefighterId,
            LocalDate dutyDate
    ) {
        super(
                "O bombeiro "
                        + firefighterId
                        + " não pode assumir o plantão de "
                        + dutyDate
                        + " porque possui outro plantão em data adjacente"
        );
    }
}