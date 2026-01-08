package ma.xproce.gestion_depenses_projet.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.xproce.gestion_depenses_projet.dao.entities.Report;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.service.ReportService;
import ma.xproce.gestion_depenses_projet.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @GetMapping
    public String showReports(@AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));


        model.addAttribute("reports", reportService.getReportsByUser(user));


        LocalDate now = LocalDate.now();
        model.addAttribute("currentYear", now.getYear());
        model.addAttribute("currentMonth", now.getMonthValue());
        model.addAttribute("defaultStartDate", now.withDayOfMonth(1));
        model.addAttribute("defaultEndDate", now);


        model.addAttribute("availableYears",
                java.util.stream.IntStream.rangeClosed(now.getYear() - 4, now.getYear())
                        .boxed()
                        .sorted(java.util.Collections.reverseOrder())
                        .toList());

        return "reports";
    }

    @PostMapping("/generate")
    public String generateReport(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam("reportType") String reportType,
                                 @RequestParam(value = "year", required = false) Integer year,
                                 @RequestParam(value = "month", required = false) Integer month,
                                 @RequestParam(value = "startDate", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                 @RequestParam(value = "endDate", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                 RedirectAttributes redirectAttributes) {

        try {
            User user = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Report report = null;

            switch (reportType) {
                case "MONTHLY":
                    if (year == null || month == null) {
                        throw new IllegalArgumentException("Année et mois requis pour rapport mensuel");
                    }
                    report = reportService.generateMonthlyReport(user, year, month);
                    break;

                case "ANNUAL":
                    if (year == null) {
                        throw new IllegalArgumentException("Année requise pour rapport annuel");
                    }
                    report = reportService.generateAnnualReport(user, year);
                    break;

                case "CUSTOM":
                    if (startDate == null || endDate == null) {
                        throw new IllegalArgumentException("Dates de début et fin requises pour rapport personnalisé");
                    }
                    if (endDate.isBefore(startDate)) {
                        throw new IllegalArgumentException("La date de fin doit être après la date de début");
                    }
                    report = reportService.generateCustomReport(user, startDate, endDate);
                    break;

                default:
                    throw new IllegalArgumentException("Type de rapport invalide");
            }

            redirectAttributes.addFlashAttribute("success",
                    "Rapport généré avec succès!");
            return "redirect:/reports/view/" + report.getId();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport", e);
            redirectAttributes.addFlashAttribute("error",
                    "Erreur lors de la génération du rapport: " + e.getMessage());
            return "redirect:/reports";
        }
    }

    @GetMapping("/view/{id}")
    public String viewReport(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Report report = reportService.getReportById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));


        if (!report.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        model.addAttribute("report", report);
        model.addAttribute("categoryDetails", reportService.getCategoryDetails(report));
        model.addAttribute("timeSeriesData", reportService.getTimeSeriesData(report));

        return "report-view";
    }

    @GetMapping("/data/{id}/categories")
    @ResponseBody
    public Map<String, Object> getCategoryData(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Report report = reportService.getReportById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (!report.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return reportService.getCategoryDetails(report);
    }

    @GetMapping("/data/{id}/timeseries")
    @ResponseBody
    public Map<String, Object> getTimeSeriesData(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Report report = reportService.getReportById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (!report.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return reportService.getTimeSeriesData(report);
    }

    @GetMapping("/export/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Report report = reportService.getReportById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));


        if (!report.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        byte[] pdfData = reportService.generatePdfReport(report);

        String filename = String.format("rapport_%s_%s_%s.pdf",
                report.getType().name().toLowerCase(),
                report.getStartDate(),
                report.getEndDate());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @PostMapping("/delete/{id}")
    public String deleteReport(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            reportService.deleteReport(user, id);

            redirectAttributes.addFlashAttribute("success",
                    "Rapport supprimé avec succès!");
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du rapport", e);
            redirectAttributes.addFlashAttribute("error",
                    "Erreur lors de la suppression du rapport");
        }

        return "redirect:/reports";
    }

    @PostMapping("/export/{id}/pdf-with-chart")
    public ResponseEntity<byte[]> exportPdfWithChart(
            @PathVariable Long id,
            @RequestBody Map<String,String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Report report = reportService.getReportById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (!report.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        String base64Image = request.get("imageBase64");
        byte[] pdfData = reportService.generatePdfReportWithChart(report, base64Image);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=rapport_" + id + "_avec_graphique.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}