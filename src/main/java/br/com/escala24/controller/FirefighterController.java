package br.com.escala24.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.escala24.dto.FirefighterRegistrationRequest;
import br.com.escala24.dto.FirefighterRegistrationResponse;
import br.com.escala24.dto.FirefighterSummaryResponse;
import br.com.escala24.service.FirefighterManagementService;
import br.com.escala24.service.FirefighterRegistrationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/firefighters")
public class FirefighterController {

    private final FirefighterRegistrationService
            registrationService;

    private final FirefighterManagementService
            managementService;

    public FirefighterController(
            FirefighterRegistrationService registrationService,
            FirefighterManagementService managementService
    ) {
        this.registrationService = registrationService;
        this.managementService = managementService;
    }

    @PostMapping
    public ResponseEntity<FirefighterRegistrationResponse>
            register(
                    @Valid @RequestBody
                    FirefighterRegistrationRequest request
            ) {
        FirefighterRegistrationResponse response =
                registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<FirefighterSummaryResponse>>
            listAll() {
        return ResponseEntity.ok(
                managementService.listAll()
        );
    }

    @PatchMapping("/{firefighterId}/deactivation")
    public ResponseEntity<FirefighterSummaryResponse>
            deactivate(
                    @PathVariable Long firefighterId
            ) {
        FirefighterSummaryResponse response =
                managementService.deactivate(
                        firefighterId
                );

        return ResponseEntity.ok(response);
    }
}