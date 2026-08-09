package br.com.escala24.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.escala24.dto.FirefighterSummaryResponse;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.exception.FirefighterNotFoundException;
import br.com.escala24.repository.FirefighterRepository;

@Service
public class FirefighterManagementService {

    private final FirefighterRepository firefighterRepository;

    public FirefighterManagementService(
            FirefighterRepository firefighterRepository) {
        this.firefighterRepository = firefighterRepository;
    }

    @Transactional(readOnly = true)
    public List<FirefighterSummaryResponse> listAll() {
        return firefighterRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FirefighterSummaryResponse deactivate(Long firefighterId) {
        Firefighter firefighter = firefighterRepository
                .findById(firefighterId)
                .orElseThrow(() ->
                        new FirefighterNotFoundException(firefighterId));

        firefighter.getUser().setActive(false);

        return toResponse(firefighter);
    }

    private FirefighterSummaryResponse toResponse(
            Firefighter firefighter) {
        return new FirefighterSummaryResponse(
                firefighter.getId(),
                firefighter.getUser().getId(),
                firefighter.getUser().getName(),
                firefighter.getUser().getEmail(),
                firefighter.getRegistration(),
                firefighter.getPhone(),
                firefighter.getUser().isActive()
        );
    }
}