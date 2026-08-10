package br.com.escala24.exception;

public class MonthlyScheduleNotFoundException
        extends RuntimeException {

    public MonthlyScheduleNotFoundException(
            int year,
            int month
    ) {
        super(
                "Escala não encontrada para o mês "
                        + month
                        + " do ano "
                        + year
        );
    }
}