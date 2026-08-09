package br.com.escala24.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.escala24.entity.Firefighter;

public interface FirefighterRepository extends JpaRepository<Firefighter, Long> {

    Optional<Firefighter> findByRegistration(String registration);

    boolean existsByRegistration(String registration);
}