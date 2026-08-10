package br.com.escala24.exception;

import java.time.LocalDate;

public class FirefighterUnavailableForDutyException
        extends RuntimeException {

    public FirefighterUnavailableForDutyException(
            Long firefighterId,
            LocalDate dutyDate
    ) {
        super(
                "O bombeiro "
                        + firefighterId
                        + " possui indisponibilidade aprovada para "
                        + dutyDate
        );
    }
}