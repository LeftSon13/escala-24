package br.com.escala24.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.escala24.entity.Firefighter;

public interface FirefighterRepository
        extends JpaRepository<Firefighter, Long> {

    @Override
    @EntityGraph(attributePaths = "user")
    List<Firefighter> findAll();

    @EntityGraph(attributePaths = "user")
    List<Firefighter> findByUserActiveTrueOrderByIdAsc();

    @EntityGraph(attributePaths = "user")
    Optional<Firefighter> findByUserEmail(String email);

    Optional<Firefighter> findByRegistration(String registration);

    boolean existsByRegistration(String registration);
}