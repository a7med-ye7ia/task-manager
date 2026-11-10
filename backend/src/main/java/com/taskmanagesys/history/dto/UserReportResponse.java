package com.taskmanagesys.history.dto;

import com.taskmanagesys.user.dto.UserSummary;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserReportResponse {
    private UserSummary user;
    private LocalDate from;
    private LocalDate to;
    private ReportSummaryDto summary;
    private List<DailyHoursDto> hoursPerDay = new ArrayList<>();
    private List<ReportTaskCardDto> taskCards = new ArrayList<>();

    public UserReportResponse() {}

    public UserSummary getUser() { return user; }
    public void setUser(UserSummary user) { this.user = user; }

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }

    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }

    public ReportSummaryDto getSummary() { return summary; }
    public void setSummary(ReportSummaryDto summary) { this.summary = summary; }

    public List<DailyHoursDto> getHoursPerDay() { return hoursPerDay; }
    public void setHoursPerDay(List<DailyHoursDto> hoursPerDay) { this.hoursPerDay = hoursPerDay; }

    public List<ReportTaskCardDto> getTaskCards() { return taskCards; }
    public void setTaskCards(List<ReportTaskCardDto> taskCards) { this.taskCards = taskCards; }
}
