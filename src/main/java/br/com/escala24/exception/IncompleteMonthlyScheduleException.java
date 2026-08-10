package br.com.escala24.exception;

public class IncompleteMonthlyScheduleException
        extends RuntimeException {

    public IncompleteMonthlyScheduleException(
            int year,
            int month,
            int expectedAssignments,
            int actualAssignments
    ) {
        super(
                "A escala do mês "
                        + month
                        + " do ano "
                        + year
                        + " está incompleta: esperados "
                        + expectedAssignments
                        + " plantões, encontrados "
                        + actualAssignments
        );
    }
}