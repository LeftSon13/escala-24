package br.com.escala24.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.escala24.entity.Role;
import br.com.escala24.entity.User;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}