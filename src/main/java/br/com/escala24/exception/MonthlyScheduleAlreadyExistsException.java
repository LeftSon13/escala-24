package br.com.escala24.exception;

public class MonthlyScheduleAlreadyExistsException
        extends RuntimeException {

    public MonthlyScheduleAlreadyExistsException(
            int year,
            int month
    ) {
        super(
                "Já existe uma escala para o mês "
                        + month
                        + " do ano "
                        + year
        );
    }
}