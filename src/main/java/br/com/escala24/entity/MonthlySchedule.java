package br.com.escala24.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "monthly_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_monthly_schedules_year_month",
                columnNames = {"schedule_year", "schedule_month"}
        )
)
public class MonthlySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_year", nullable = false)
    private int year;

    @Column(name = "schedule_month", nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MonthlySchedule() {
    }

    public Long getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}