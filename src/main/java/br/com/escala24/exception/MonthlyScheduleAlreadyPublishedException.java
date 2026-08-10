package br.com.escala24.exception;

public class MonthlyScheduleAlreadyPublishedException
        extends RuntimeException {

    public MonthlyScheduleAlreadyPublishedException(
            int year,
            int month
    ) {
        super(
                "A escala do mês "
                        + month
                        + " do ano "
                        + year
                        + " já está publicada"
        );
    }
}