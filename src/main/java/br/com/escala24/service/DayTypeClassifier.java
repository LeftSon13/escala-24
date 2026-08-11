package br.com.escala24.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.DayType;
import br.com.escala24.repository.HolidayRepository;

@Service
public class DayTypeClassifier {

    private final HolidayRepository holidayRepository;

    public DayTypeClassifier(
            HolidayRepository holidayRepository
    ) {
        this.holidayRepository = holidayRepository;
    }

    @Transactional(readOnly = true)
    public DayType classify(LocalDate date) {
        Objects.requireNonNull(
                date,
                "A data é obrigatória"
        );

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY
                || dayOfWeek == DayOfWeek.SUNDAY) {
            return DayType.WEEKEND_OR_HOLIDAY;
        }

        if (holidayRepository.existsByDate(date)) {
            return DayType.WEEKEND_OR_HOLIDAY;
        }

        return DayType.WEEKDAY;
    }
}