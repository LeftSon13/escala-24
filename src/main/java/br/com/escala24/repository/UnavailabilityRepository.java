package br.com.escala24.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.escala24.entity.Unavailability;
import br.com.escala24.entity.UnavailabilityStatus;

public interface UnavailabilityRepository
        extends JpaRepository<Unavailability, Long> {

    @EntityGraph(attributePaths = {"firefighter", "firefighter.user"})
    List<Unavailability> findByStatusOrderByRequestedAtAsc(
            UnavailabilityStatus status
    );

    @EntityGraph(attributePaths = {"firefighter", "firefighter.user"})
    List<Unavailability> findByFirefighterIdOrderByStartDateDesc(
            Long firefighterId
    );

    boolean
            existsByFirefighterIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    Long firefighterId,
                    UnavailabilityStatus status,
                    LocalDate dateForStartLimit,
                    LocalDate dateForEndLimit
            );

    @EntityGraph(attributePaths = {"firefighter", "firefighter.user"})
    List<Unavailability>
            findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    UnavailabilityStatus status,
                    LocalDate periodEnd,
                    LocalDate periodStart
            );
}