package br.com.escala24.exception;

import java.time.LocalDate;

public class NoEligibleFirefighterException
        extends RuntimeException {

    public NoEligibleFirefighterException(LocalDate dutyDate) {
        super(
                "Nenhum bombeiro elegível para o plantão do dia "
                        + dutyDate
        );
    }
}