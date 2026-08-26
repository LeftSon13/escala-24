package br.com.escala24.service;

import java.util.Locale;

public enum MonthlySchedulePdfLayout {
    LIST,
    CALENDAR;

    public static MonthlySchedulePdfLayout from(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
