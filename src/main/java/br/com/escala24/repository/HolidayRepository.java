package br.com.escala24.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.escala24.entity.Holiday;

public interface HolidayRepository
        extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByDate(LocalDate date);

    boolean existsByDate(LocalDate date);

    List<Holiday> findByDateBetweenOrderByDateAsc(
            LocalDate startDate,
            LocalDate endDate
    );
}