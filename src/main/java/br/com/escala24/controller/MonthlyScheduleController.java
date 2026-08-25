package br.com.escala24.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.escala24.dto.DutyAssignmentResponse;
import br.com.escala24.dto.DutyReassignmentRequest;
import br.com.escala24.dto.MonthlyScheduleGenerationRequest;
import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.service.DutyReassignmentService;
import br.com.escala24.service.MonthlyScheduleGenerationService;
import br.com.escala24.service.MonthlyScheduleManagementService;
import br.com.escala24.service.MonthlySchedulePdfService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/monthly-schedules")
@Validated
public class MonthlyScheduleController {

    private final MonthlyScheduleGenerationService generationService;
    private final MonthlyScheduleManagementService managementService;
    private final DutyReassignmentService reassignmentService;
    private final MonthlySchedulePdfService pdfService;

    public MonthlyScheduleController(
            MonthlyScheduleGenerationService generationService,
            MonthlyScheduleManagementService managementService,
            DutyReassignmentService reassignmentService,
            MonthlySchedulePdfService pdfService
    ) {
        this.generationService = generationService;
        this.managementService = managementService;
        this.reassignmentService = reassignmentService;
        this.pdfService = pdfService;
    }

    @GetMapping(
            value = "/{year}/{month}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable int year,
            @PathVariable int month
    ) {
        byte[] pdf = pdfService.exportPublishedSchedule(year, month);
        String filename = "escala-%04d-%02d.pdf".formatted(
                year,
                month
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping
    public ResponseEntity<MonthlyScheduleGenerationResponse> generate(
            @Valid @RequestBody
            MonthlyScheduleGenerationRequest request
    ) {
        MonthlyScheduleGenerationResponse response =
                generationService.generate(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{year}/{month}")
    public ResponseEntity<MonthlyScheduleGenerationResponse> find(
            @PathVariable int year,
            @PathVariable int month
    ) {
        MonthlyScheduleGenerationResponse response =
                managementService.findByYearAndMonth(
                        year,
                        month
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{year}/{month}/publication")
    public ResponseEntity<MonthlyScheduleGenerationResponse> publish(
            @PathVariable int year,
            @PathVariable int month
    ) {
        MonthlyScheduleGenerationResponse response =
                managementService.publish(year, month);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{year}/{month}/assignments/{dutyDate}")
    public ResponseEntity<DutyAssignmentResponse> reassign(
            @PathVariable int year,
            @PathVariable int month,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dutyDate,
            @Valid @RequestBody
            DutyReassignmentRequest request
    ) {
        DutyAssignmentResponse response =
                reassignmentService.reassign(
                        year,
                        month,
                        dutyDate,
                        request
                );

        return ResponseEntity.ok(response);
    }
}
