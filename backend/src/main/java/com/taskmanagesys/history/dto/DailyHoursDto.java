package com.taskmanagesys.history.dto;

import java.time.LocalDate;

public class DailyHoursDto {
    private LocalDate date;
    private long workedSeconds;
    private double hours;

    public DailyHoursDto() {}

    public DailyHoursDto(LocalDate date, long workedSeconds, double hours) {
        this.date = date;
        this.workedSeconds = workedSeconds;
        this.hours = hours;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public long getWorkedSeconds() { return workedSeconds; }
    public void setWorkedSeconds(long workedSeconds) { this.workedSeconds = workedSeconds; }

    public double getHours() { return hours; }
    public void setHours(double hours) { this.hours = hours; }
}
