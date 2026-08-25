package br.com.escala24.exception;

public class UnpublishedScheduleExportException
        extends RuntimeException {

    public UnpublishedScheduleExportException(
            int year,
            int month
    ) {
        super(
                "A escala de "
                        + month
                        + "/"
                        + year
                        + " precisa estar publicada para ser exportada"
        );
    }
}
