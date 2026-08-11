package br.com.escala24.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.escala24.dto.HolidayRequest;
import br.com.escala24.dto.HolidayResponse;
import br.com.escala24.exception.HolidayAlreadyExistsException;
import br.com.escala24.exception.HolidayNotFoundException;
import br.com.escala24.service.HolidayManagementService;

@WebMvcTest(HolidayController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HolidayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HolidayManagementService holidayService;

    @Test
    void shouldCreateHoliday() throws Exception {
        LocalDate holidayDate = LocalDate.of(
                2027,
                9,
                7
        );

        HolidayResponse response = new HolidayResponse(
                1L,
                holidayDate,
                "Independência do Brasil",
                LocalDateTime.of(
                        2027,
                        1,
                        10,
                        9,
                        30
                )
        );

        when(holidayService.create(
                new HolidayRequest(
                        holidayDate,
                        "Independência do Brasil"
                )
        )).thenReturn(response);

        mockMvc.perform(
                post("/api/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2027-09-07",
                                  "name": "Independência do Brasil"
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(
                        jsonPath("$.date")
                                .value("2027-09-07")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Independência do Brasil")
                );

        verify(holidayService).create(
                new HolidayRequest(
                        holidayDate,
                        "Independência do Brasil"
                )
        );
    }

    @Test
    void shouldListHolidaysByYear() throws Exception {
        when(holidayService.listByYear(2027))
                .thenReturn(
                        List.of(
                                new HolidayResponse(
                                        2L,
                                        LocalDate.of(
                                                2027,
                                                1,
                                                1
                                        ),
                                        "Confraternização Universal",
                                        LocalDateTime.of(
                                                2026,
                                                12,
                                                1,
                                                10,
                                                0
                                        )
                                ),
                                new HolidayResponse(
                                        3L,
                                        LocalDate.of(
                                                2027,
                                                12,
                                                25
                                        ),
                                        "Natal",
                                        LocalDateTime.of(
                                                2026,
                                                12,
                                                1,
                                                10,
                                                5
                                        )
                                )
                        )
                );

        mockMvc.perform(
                get("/api/holidays")
                        .param("year", "2027")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(
                        jsonPath("$[0].date")
                                .value("2027-01-01")
                )
                .andExpect(
                        jsonPath("$[0].name")
                                .value("Confraternização Universal")
                )
                .andExpect(
                        jsonPath("$[1].date")
                                .value("2027-12-25")
                )
                .andExpect(
                        jsonPath("$[1].name")
                                .value("Natal")
                );

        verify(holidayService).listByYear(2027);
    }

    @Test
    void shouldUpdateHoliday() throws Exception {
        LocalDate holidayDate = LocalDate.of(
                2027,
                4,
                21
        );

        HolidayResponse response = new HolidayResponse(
                4L,
                holidayDate,
                "Tiradentes",
                LocalDateTime.of(
                        2026,
                        12,
                        1,
                        11,
                        0
                )
        );

        when(holidayService.update(
                4L,
                new HolidayRequest(
                        holidayDate,
                        "Tiradentes"
                )
        )).thenReturn(response);

        mockMvc.perform(
                put("/api/holidays/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2027-04-21",
                                  "name": "Tiradentes"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(
                        jsonPath("$.date")
                                .value("2027-04-21")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Tiradentes")
                );

        verify(holidayService).update(
                4L,
                new HolidayRequest(
                        holidayDate,
                        "Tiradentes"
                )
        );
    }

    @Test
    void shouldDeleteHoliday() throws Exception {
        mockMvc.perform(
                delete("/api/holidays/5")
        )
                .andExpect(status().isNoContent());

        verify(holidayService).delete(5L);
    }

    @Test
    void shouldReturnValidationErrors() throws Exception {
        mockMvc.perform(
                post("/api/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": null,
                                  "name": "   "
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "A requisição possui campos inválidos"
                                )
                )
                .andExpect(
                        jsonPath("$.fieldErrors.date")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.fieldErrors.name")
                                .exists()
                );
    }

    @Test
    void shouldReturnConflictForDuplicatedDate()
            throws Exception {
        LocalDate holidayDate = LocalDate.of(
                2027,
                9,
                7
        );

        when(holidayService.create(
                new HolidayRequest(
                        holidayDate,
                        "Independência do Brasil"
                )
        )).thenThrow(
                new HolidayAlreadyExistsException(
                        holidayDate
                )
        );

        mockMvc.perform(
                post("/api/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2027-09-07",
                                  "name": "Independência do Brasil"
                                }
                                """)
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/holidays")
                );
    }

    @Test
    void shouldReturnNotFoundForMissingHoliday()
            throws Exception {
        doThrow(new HolidayNotFoundException(99L))
                .when(holidayService)
                .delete(99L);

        mockMvc.perform(
                delete("/api/holidays/99")
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/holidays/99")
                );
    }
}