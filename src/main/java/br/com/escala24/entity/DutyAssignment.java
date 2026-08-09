package br.com.escala24.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "duty_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_duty_assignments_schedule_date",
                columnNames = {"monthly_schedule_id", "duty_date"}
        )
)
public class DutyAssignment {

    private static final LocalTime DUTY_START_TIME = LocalTime.of(8, 0);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_schedule_id", nullable = false)
    private MonthlySchedule monthlySchedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "firefighter_id", nullable = false)
    private Firefighter firefighter;

    @Column(name = "duty_date", nullable = false)
    private LocalDate dutyDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 30)
    private DayType dayType;

    public DutyAssignment() {
    }

    public Long getId() {
        return id;
    }

    public MonthlySchedule getMonthlySchedule() {
        return monthlySchedule;
    }

    public void setMonthlySchedule(MonthlySchedule monthlySchedule) {
        this.monthlySchedule = monthlySchedule;
    }

    public Firefighter getFirefighter() {
        return firefighter;
    }

    public void setFirefighter(Firefighter firefighter) {
        this.firefighter = firefighter;
    }

    public LocalDate getDutyDate() {
        return dutyDate;
    }

    public void setDutyDate(LocalDate dutyDate) {
        this.dutyDate = dutyDate;
    }

    public DayType getDayType() {
        return dayType;
    }

    public void setDayType(DayType dayType) {
        this.dayType = dayType;
    }

    @Transient
    public LocalDateTime getStartDateTime() {
        return dutyDate.atTime(DUTY_START_TIME);
    }

    @Transient
    public LocalDateTime getEndDateTime() {
        return dutyDate.plusDays(1).atTime(DUTY_START_TIME);
    }
}