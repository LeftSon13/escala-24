package br.com.escala24.service;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import br.com.escala24.dto.HolidayRequest;
import br.com.escala24.dto.HolidayResponse;
import br.com.escala24.entity.Holiday;
import br.com.escala24.exception.HolidayAlreadyExistsException;
import br.com.escala24.exception.HolidayNotFoundException;
import br.com.escala24.repository.HolidayRepository;
import jakarta.validation.Valid;

@Service
@Validated
public class HolidayManagementService {

    private final HolidayRepository holidayRepository;

    public HolidayManagementService(
            HolidayRepository holidayRepository
    ) {
        this.holidayRepository = holidayRepository;
    }

    @Transactional
    public HolidayResponse create(
            @Valid HolidayRequest request
    ) {
        validateAvailableDate(request.date(), null);

        Holiday holiday = new Holiday();
        holiday.setDate(request.date());
        holiday.setName(normalizeName(request.name()));

        Holiday savedHoliday = holidayRepository.save(holiday);

        return toResponse(savedHoliday);
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> listByYear(int year) {
        Year requestedYear = Year.of(year);

        LocalDate startDate = requestedYear.atDay(1);
        LocalDate endDate = requestedYear.atMonth(12).atEndOfMonth();

        return holidayRepository
                .findByDateBetweenOrderByDateAsc(
                        startDate,
                        endDate
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HolidayResponse update(
            Long holidayId,
            @Valid HolidayRequest request
    ) {
        Holiday holiday = findById(holidayId);

        validateAvailableDate(
                request.date(),
                holidayId
        );

        holiday.setDate(request.date());
        holiday.setName(normalizeName(request.name()));

        return toResponse(holiday);
    }

    @Transactional
    public void delete(Long holidayId) {
        Holiday holiday = findById(holidayId);

        holidayRepository.delete(holiday);
    }

    private Holiday findById(Long holidayId) {
        return holidayRepository
                .findById(holidayId)
                .orElseThrow(
                        () -> new HolidayNotFoundException(
                                holidayId
                        )
                );
    }

    private void validateAvailableDate(
            LocalDate date,
            Long ignoredHolidayId
    ) {
        holidayRepository
                .findByDate(date)
                .filter(existingHoliday ->
                        !existingHoliday
                                .getId()
                                .equals(ignoredHolidayId)
                )
                .ifPresent(existingHoliday -> {
                    throw new HolidayAlreadyExistsException(date);
                });
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private HolidayResponse toResponse(Holiday holiday) {
        return new HolidayResponse(
                holiday.getId(),
                holiday.getDate(),
                holiday.getName(),
                holiday.getCreatedAt()
        );
    }
}