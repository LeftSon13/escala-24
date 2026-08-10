package br.com.escala24.exception;

public class PublishedScheduleModificationException
        extends RuntimeException {

    public PublishedScheduleModificationException(
            int year,
            int month
    ) {
        super(
                "A escala de "
                        + month
                        + "/"
                        + year
                        + " já foi publicada e não pode ser alterada"
        );
    }
}