package br.com.escala24.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.escala24.entity.DayType;
import br.com.escala24.entity.DutyAssignment;

public interface DutyAssignmentRepository
                extends JpaRepository<DutyAssignment, Long> {

        @EntityGraph(attributePaths = { "firefighter", "firefighter.user" })
        List<DutyAssignment> findByMonthlyScheduleIdOrderByDutyDateAsc(
                        Long monthlyScheduleId);

        boolean existsByMonthlyScheduleIdAndDutyDate(
                        Long monthlyScheduleId,
                        LocalDate dutyDate);

        Optional<DutyAssignment> findTopByFirefighterIdAndDutyDateBeforeOrderByDutyDateDesc(
                        Long firefighterId,
                        LocalDate dutyDate);

        long countByFirefighterIdAndDayTypeAndDutyDateBetween(
                        Long firefighterId,
                        DayType dayType,
                        LocalDate startDate,
                        LocalDate endDate);

        boolean existsByFirefighterIdAndDutyDateBetween(
                        Long firefighterId,
                        LocalDate startDate,
                        LocalDate endDate);
}