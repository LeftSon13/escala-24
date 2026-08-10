package br.com.escala24.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import br.com.escala24.dto.UnavailabilityRequest;
import br.com.escala24.dto.UnavailabilityResponse;
import br.com.escala24.dto.UnavailabilityReviewResponse;
import br.com.escala24.entity.Firefighter;
import br.com.escala24.entity.Role;
import br.com.escala24.entity.Unavailability;
import br.com.escala24.entity.UnavailabilityStatus;
import br.com.escala24.entity.User;
import br.com.escala24.exception.AdministratorRequiredException;
import br.com.escala24.exception.DutyReassignmentRequiredException;
import br.com.escala24.exception.FirefighterNotFoundException;
import br.com.escala24.exception.InvalidUnavailabilityPeriodException;
import br.com.escala24.exception.UnavailabilityAlreadyReviewedException;
import br.com.escala24.exception.UnavailabilityNotFoundException;
import br.com.escala24.repository.DutyAssignmentRepository;
import br.com.escala24.repository.FirefighterRepository;
import br.com.escala24.repository.UnavailabilityRepository;
import br.com.escala24.repository.UserRepository;
import jakarta.validation.Valid;

@Service
@Validated
public class UnavailabilityManagementService {

        private final UnavailabilityRepository unavailabilityRepository;
        private final FirefighterRepository firefighterRepository;
        private final UserRepository userRepository;
        private final DutyAssignmentRepository dutyAssignmentRepository;

        public UnavailabilityManagementService(
                        UnavailabilityRepository unavailabilityRepository,
                        FirefighterRepository firefighterRepository,
                        UserRepository userRepository,
                        DutyAssignmentRepository dutyAssignmentRepository) {
                this.unavailabilityRepository = unavailabilityRepository;
                this.firefighterRepository = firefighterRepository;
                this.userRepository = userRepository;
                this.dutyAssignmentRepository = dutyAssignmentRepository;
        }

        @Transactional
        public UnavailabilityResponse request(
                        Long firefighterId,
                        @Valid UnavailabilityRequest request) {
                validatePeriod(request);

                Firefighter firefighter = firefighterRepository
                                .findById(firefighterId)
                                .filter(foundFirefighter -> foundFirefighter.getUser().isActive())
                                .orElseThrow(() -> new FirefighterNotFoundException(firefighterId));

                Unavailability unavailability = new Unavailability();
                unavailability.setFirefighter(firefighter);
                unavailability.setType(request.type());
                unavailability.setStartDate(request.startDate());
                unavailability.setEndDate(request.endDate());
                unavailability.setReason(normalizeReason(request.reason()));

                Unavailability savedUnavailability = unavailabilityRepository.save(unavailability);

                return toResponse(savedUnavailability);
        }

        @Transactional
        public UnavailabilityResponse requestForAuthenticatedFirefighter(
                        String authenticatedEmail,
                        @Valid UnavailabilityRequest request) {
                Firefighter firefighter = findAuthenticatedFirefighter(
                                authenticatedEmail);

                return request(
                                firefighter.getId(),
                                request);
        }

        @Transactional(readOnly = true)
        public List<UnavailabilityResponse> listForAuthenticatedFirefighter(
                        String authenticatedEmail) {
                Firefighter firefighter = findAuthenticatedFirefighter(
                                authenticatedEmail);

                return unavailabilityRepository
                                .findByFirefighterIdOrderByStartDateDesc(
                                                firefighter.getId())
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional
        public UnavailabilityResponse approveForAuthenticatedAdministrator(
                        Long unavailabilityId,
                        String authenticatedEmail) {
                User administrator = findAuthenticatedAdministrator(
                                authenticatedEmail);

                return approve(
                                unavailabilityId,
                                administrator.getId());
        }

        @Transactional
        public UnavailabilityResponse rejectForAuthenticatedAdministrator(
                        Long unavailabilityId,
                        String authenticatedEmail) {
                User administrator = findAuthenticatedAdministrator(
                                authenticatedEmail);

                return reject(
                                unavailabilityId,
                                administrator.getId());
        }

        @Transactional(readOnly = true)
        public List<UnavailabilityReviewResponse> listPending() {
                return unavailabilityRepository
                                .findByStatusOrderByRequestedAtAsc(
                                                UnavailabilityStatus.PENDING)
                                .stream()
                                .map(this::toReviewResponse)
                                .toList();
        }

        @Transactional
        public UnavailabilityResponse approve(
                        Long unavailabilityId,
                        Long administratorUserId) {
                return review(
                                unavailabilityId,
                                administratorUserId,
                                UnavailabilityStatus.APPROVED);
        }

        @Transactional
        public UnavailabilityResponse reject(
                        Long unavailabilityId,
                        Long administratorUserId) {
                return review(
                                unavailabilityId,
                                administratorUserId,
                                UnavailabilityStatus.REJECTED);
        }

        private UnavailabilityResponse review(
                        Long unavailabilityId,
                        Long administratorUserId,
                        UnavailabilityStatus decision) {
                Unavailability unavailability = unavailabilityRepository
                                .findById(unavailabilityId)
                                .orElseThrow(() -> new UnavailabilityNotFoundException(
                                                unavailabilityId));

                if (unavailability.getStatus() != UnavailabilityStatus.PENDING) {
                        throw new UnavailabilityAlreadyReviewedException(
                                        unavailabilityId);
                }

                User administrator = userRepository
                                .findById(administratorUserId)
                                .filter(User::isActive)
                                .filter(user -> user.getRole() == Role.ADMIN)
                                .orElseThrow(AdministratorRequiredException::new);

                if (decision == UnavailabilityStatus.APPROVED
                                && dutyAssignmentRepository
                                                .existsByFirefighterIdAndDutyDateBetween(
                                                                unavailability.getFirefighter().getId(),
                                                                unavailability.getStartDate(),
                                                                unavailability.getEndDate())) {
                        throw new DutyReassignmentRequiredException();
                }

                unavailability.setStatus(decision);
                unavailability.setReviewedBy(administrator);
                unavailability.setReviewedAt(LocalDateTime.now());

                return toResponse(unavailability);
        }

        private Firefighter findAuthenticatedFirefighter(
                        String authenticatedEmail) {
                String normalizedEmail = authenticatedEmail
                                .trim()
                                .toLowerCase(Locale.ROOT);

                return firefighterRepository
                                .findByUserEmail(normalizedEmail)
                                .filter(firefighter -> firefighter.getUser().isActive())
                                .orElseThrow(() -> new IllegalStateException(
                                                "Bombeiro autenticado não encontrado"));
        }

        private User findAuthenticatedAdministrator(
                        String authenticatedEmail) {
                String normalizedEmail = authenticatedEmail
                                .trim()
                                .toLowerCase(Locale.ROOT);

                return userRepository
                                .findByEmail(normalizedEmail)
                                .filter(User::isActive)
                                .filter(user -> user.getRole() == Role.ADMIN)
                                .orElseThrow(
                                                AdministratorRequiredException::new);
        }

        private void validatePeriod(UnavailabilityRequest request) {
                if (request.endDate().isBefore(request.startDate())) {
                        throw new InvalidUnavailabilityPeriodException();
                }
        }

        private String normalizeReason(String reason) {
                if (reason == null || reason.isBlank()) {
                        return null;
                }

                return reason.trim();
        }

        private UnavailabilityResponse toResponse(
                        Unavailability unavailability) {
                return new UnavailabilityResponse(
                                unavailability.getId(),
                                unavailability.getFirefighter().getId(),
                                unavailability.getFirefighter()
                                                .getUser()
                                                .getName(),
                                unavailability.getType(),
                                unavailability.getStartDate(),
                                unavailability.getEndDate(),
                                unavailability.getStatus(),
                                unavailability.getRequestedAt(),
                                unavailability.getReviewedAt());
        }

        private UnavailabilityReviewResponse toReviewResponse(
                        Unavailability unavailability) {
                return new UnavailabilityReviewResponse(
                                unavailability.getId(),
                                unavailability.getFirefighter().getId(),
                                unavailability.getFirefighter()
                                                .getUser()
                                                .getName(),
                                unavailability.getType(),
                                unavailability.getStartDate(),
                                unavailability.getEndDate(),
                                unavailability.getReason(),
                                unavailability.getStatus(),
                                unavailability.getRequestedAt());
        }
}