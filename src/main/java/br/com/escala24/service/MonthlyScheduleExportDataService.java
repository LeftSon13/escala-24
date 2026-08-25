package br.com.escala24.service;

import org.springframework.stereotype.Service;

import br.com.escala24.dto.MonthlyScheduleGenerationResponse;
import br.com.escala24.entity.ScheduleStatus;
import br.com.escala24.exception.UnpublishedScheduleExportException;

@Service
public class MonthlyScheduleExportDataService {

    private final MonthlyScheduleManagementService managementService;

    public MonthlyScheduleExportDataService(
            MonthlyScheduleManagementService managementService
    ) {
        this.managementService = managementService;
    }

    public MonthlyScheduleGenerationResponse findPublished(
            int year,
            int month
    ) {
        MonthlyScheduleGenerationResponse schedule =
                managementService.findByYearAndMonth(year, month);

        if (schedule.status() != ScheduleStatus.PUBLISHED) {
            throw new UnpublishedScheduleExportException(year, month);
        }

        return schedule;
    }
}
