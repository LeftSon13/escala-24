package br.com.escala24.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.entity.User;
import br.com.escala24.repository.UserRepository;

@Service
public class DatabaseUserDetailsService
                implements UserDetailsService {

        private final UserRepository userRepository;

        public DatabaseUserDetailsService(
                        UserRepository userRepository) {
                this.userRepository = userRepository;
        }

        @Override
        @Transactional(readOnly = true)
        public UserDetails loadUserByUsername(String username) {
                String normalizedEmail = username
                                .trim()
                                .toLowerCase(Locale.ROOT);

                User user = userRepository
                                .findByEmail(normalizedEmail)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "Credenciais inválidas"));

                org.springframework.security.core.userdetails.User.UserBuilder userDetails = org.springframework.security.core.userdetails.User
                                .withUsername(user.getEmail())
                                .password(user.getPassword())
                                .disabled(!user.isActive());

                if (user.isMustChangePassword()) {
                        return userDetails
                                        .authorities(
                                                        "PASSWORD_CHANGE_REQUIRED")
                                        .build();
                }

                return userDetails
                                .roles(user.getRole().name())
                                .build();
        }
}