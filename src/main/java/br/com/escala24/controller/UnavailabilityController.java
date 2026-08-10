package br.com.escala24.controller;

import java.security.Principal;
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

import br.com.escala24.dto.UnavailabilityRequest;
import br.com.escala24.dto.UnavailabilityResponse;
import br.com.escala24.dto.UnavailabilityReviewResponse;
import br.com.escala24.service.UnavailabilityManagementService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/unavailabilities")
public class UnavailabilityController {

    private final UnavailabilityManagementService managementService;

    public UnavailabilityController(
            UnavailabilityManagementService managementService) {
        this.managementService = managementService;
    }

    @PostMapping
    public ResponseEntity<UnavailabilityResponse> request(
            Principal principal,
            @Valid @RequestBody UnavailabilityRequest request) {
        UnavailabilityResponse response = managementService
                .requestForAuthenticatedFirefighter(
                        principal.getName(),
                        request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<UnavailabilityResponse>> listMine(Principal principal) {
        return ResponseEntity.ok(
                managementService
                        .listForAuthenticatedFirefighter(
                                principal.getName()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<UnavailabilityReviewResponse>> listPending() {
        return ResponseEntity.ok(
                managementService.listPending());
    }

    @PatchMapping("/{unavailabilityId}/approval")
    public ResponseEntity<UnavailabilityResponse> approve(
            @PathVariable Long unavailabilityId,
            Principal principal) {
        UnavailabilityResponse response = managementService
                .approveForAuthenticatedAdministrator(
                        unavailabilityId,
                        principal.getName());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{unavailabilityId}/rejection")
    public ResponseEntity<UnavailabilityResponse> reject(
            @PathVariable Long unavailabilityId,
            Principal principal) {
        UnavailabilityResponse response = managementService
                .rejectForAuthenticatedAdministrator(
                        unavailabilityId,
                        principal.getName());

        return ResponseEntity.ok(response);
    }
}