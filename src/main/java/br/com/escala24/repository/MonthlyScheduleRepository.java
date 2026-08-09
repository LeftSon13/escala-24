package br.com.escala24.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.escala24.entity.MonthlySchedule;

public interface MonthlyScheduleRepository
        extends JpaRepository<MonthlySchedule, Long> {

    Optional<MonthlySchedule> findByYearAndMonth(int year, int month);

    boolean existsByYearAndMonth(int year, int month);
}