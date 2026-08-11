package br.com.escala24.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.escala24.dto.HolidayRequest;
import br.com.escala24.dto.HolidayResponse;
import br.com.escala24.service.HolidayManagementService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayManagementService holidayService;

    public HolidayController(
            HolidayManagementService holidayService
    ) {
        this.holidayService = holidayService;
    }

    @PostMapping
    public ResponseEntity<HolidayResponse> create(
            @Valid @RequestBody HolidayRequest request
    ) {
        HolidayResponse response =
                holidayService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<HolidayResponse>> listByYear(
            @RequestParam int year
    ) {
        return ResponseEntity.ok(
                holidayService.listByYear(year)
        );
    }

    @PutMapping("/{holidayId}")
    public ResponseEntity<HolidayResponse> update(
            @PathVariable Long holidayId,
            @Valid @RequestBody HolidayRequest request
    ) {
        return ResponseEntity.ok(
                holidayService.update(
                        holidayId,
                        request
                )
        );
    }

    @DeleteMapping("/{holidayId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long holidayId
    ) {
        holidayService.delete(holidayId);

        return ResponseEntity.noContent().build();
    }
}