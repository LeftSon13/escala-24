package br.com.escala24.exception;

import java.time.LocalDate;

public class DutyAssignmentNotFoundException
        extends RuntimeException {

    public DutyAssignmentNotFoundException(
            int year,
            int month,
            LocalDate dutyDate
    ) {
        super(
                "Plantão não encontrado na escala de "
                        + month
                        + "/"
                        + year
                        + " para a data "
                        + dutyDate
        );
    }
}