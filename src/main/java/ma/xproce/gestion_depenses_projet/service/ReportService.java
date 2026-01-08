package ma.xproce.gestion_depenses_projet.service;

import ma.xproce.gestion_depenses_projet.dao.entities.Report;
import ma.xproce.gestion_depenses_projet.dao.entities.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReportService {

    Report generateMonthlyReport(User user, int year, int month);

    Report generateAnnualReport(User user, int year);

    Report generateCustomReport(User user, LocalDate startDate, LocalDate endDate);

    List<Report> getReportsByUser(User user);

    Optional<Report> getReportById(Long id);

    void deleteReport(User user, Long id);

    Map<String, Object> getCategoryDetails(Report report);

    Map<String, Object> getTimeSeriesData(Report report);

    byte[] generatePdfReport(Report report);

    public byte[] generatePdfReportWithChart(Report report, String base64Chart);
}