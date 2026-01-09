package ma.xproce.gestion_depenses_projet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Div;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.xproce.gestion_depenses_projet.dao.entities.*;
import ma.xproce.gestion_depenses_projet.dao.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Text;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReportManager implements ReportService {

    private final ReportRepository reportRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Report generateMonthlyReport(User user, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        return generateReport(user, startDate, endDate, Report.ReportType.MONTHLY);
    }

    @Override
    @Transactional
    public Report generateAnnualReport(User user, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return generateReport(user, startDate, endDate, Report.ReportType.ANNUAL);
    }

    @Override
    @Transactional
    public Report generateCustomReport(User user, LocalDate startDate, LocalDate endDate) {
        return generateReport(user, startDate, endDate, Report.ReportType.CUSTOM);
    }

    private Report generateReport(User user, LocalDate startDate, LocalDate endDate, Report.ReportType type) {

        Optional<Report> existingReport = reportRepository.findByUserAndPeriod(user, startDate, endDate);
        if (existingReport.isPresent()) {
            return existingReport.get();
        }


        List<Expense> expenses = expenseRepository.findByUserAndDateBetween(user, startDate, endDate);


        List<Category> categories = categoryService.getUserCategories(user);


        Map<String, CategoryData> categoryDataMap = new HashMap<>();
        Map<LocalDate, BigDecimal> dailyExpenses = new TreeMap<>();
        Map<YearMonth, BigDecimal> monthlyBudgets = new HashMap<>();


        for (Category category : categories) {
            categoryDataMap.put(category.getName(), new CategoryData(category.getName()));
        }


        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            YearMonth yearMonth = YearMonth.from(currentDate);

            for (Category category : categories) {
                Budget budget = budgetRepository.findByUserAndCategoryAndMonth(user, category, yearMonth)
                        .orElse(null);

                if (budget != null) {
                    CategoryData catData = categoryDataMap.get(category.getName());
                    catData.budget = catData.budget.add(budget.getAmount());

                    monthlyBudgets.merge(yearMonth, budget.getAmount(), BigDecimal::add);
                }
            }
            currentDate = currentDate.plusMonths(1);
        }


        for (Expense expense : expenses) {
            String categoryName = expense.getCategory().getName();
            CategoryData catData = categoryDataMap.computeIfAbsent(categoryName,
                    k -> new CategoryData(categoryName));

            catData.expenses = catData.expenses.add(expense.getAmount());
            catData.transactionCount++;


            dailyExpenses.merge(expense.getDate(), expense.getAmount(), BigDecimal::add);
        }


        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal totalOverBudget = BigDecimal.ZERO;
        int overBudgetCount = 0;
        String dominantCategory = "";
        BigDecimal dominantAmount = BigDecimal.ZERO;

        List<Map<String, Object>> categoryDetailsList = new ArrayList<>();

        for (CategoryData catData : categoryDataMap.values()) {
            totalBudget = totalBudget.add(catData.budget);
            totalExpenses = totalExpenses.add(catData.expenses);


            if (catData.budget.compareTo(BigDecimal.ZERO) > 0) {
                catData.usagePercentage = catData.expenses
                        .divide(catData.budget, 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();


                if (catData.expenses.compareTo(catData.budget) > 0) {
                    catData.overBudgetAmount = catData.expenses.subtract(catData.budget);
                    totalOverBudget = totalOverBudget.add(catData.overBudgetAmount);
                    overBudgetCount++;
                }
            } else if (catData.expenses.compareTo(BigDecimal.ZERO) > 0) {
                catData.usagePercentage = 100.0;
                catData.overBudgetAmount = catData.expenses;
                totalOverBudget = totalOverBudget.add(catData.overBudgetAmount);
                overBudgetCount++;
            }


            if (catData.expenses.compareTo(dominantAmount) > 0) {
                dominantCategory = catData.categoryName;
                dominantAmount = catData.expenses;
            }


            Map<String, Object> details = new HashMap<>();
            details.put("name", catData.categoryName);
            details.put("budget", catData.budget);
            details.put("expenses", catData.expenses);
            details.put("usagePercentage", catData.usagePercentage);
            details.put("overBudgetAmount", catData.overBudgetAmount);
            details.put("transactionCount", catData.transactionCount);
            categoryDetailsList.add(details);
        }


        List<Map<String, Object>> timeSeriesData = generateTimeSeries(
                dailyExpenses, monthlyBudgets, startDate, endDate, type);


        BigDecimal totalSavings = totalBudget.subtract(totalExpenses);
        Double globalUsagePercentage = totalBudget.compareTo(BigDecimal.ZERO) > 0
                ? totalExpenses.divide(totalBudget, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;


        try {
            Report report = Report.builder()
                    .user(user)
                    .type(type)
                    .startDate(startDate)
                    .endDate(endDate)
                    .generatedAt(LocalDateTime.now())
                    .totalBudget(totalBudget)
                    .totalExpenses(totalExpenses)
                    .totalSavings(totalSavings)
                    .globalUsagePercentage(globalUsagePercentage)
                    .dominantCategory(dominantCategory)
                    .dominantCategoryAmount(dominantAmount)
                    .overBudgetCategoriesCount(overBudgetCount)
                    .totalOverBudgetAmount(totalOverBudget)
                    .categoryDetailsJson(objectMapper.writeValueAsString(categoryDetailsList))
                    .timeSeriesJson(objectMapper.writeValueAsString(timeSeriesData))
                    .build();

            return reportRepository.save(report);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    private List<Map<String, Object>> generateTimeSeries(Map<LocalDate, BigDecimal> dailyExpenses,
                                                         Map<YearMonth, BigDecimal> monthlyBudgets,
                                                         LocalDate startDate, LocalDate endDate,
                                                         Report.ReportType type) {
        List<Map<String, Object>> series = new ArrayList<>();
        BigDecimal cumulativeExpenses = BigDecimal.ZERO;
        BigDecimal cumulativeBudget = BigDecimal.ZERO;

        if (type == Report.ReportType.ANNUAL) {

            YearMonth currentMonth = YearMonth.from(startDate);
            YearMonth endMonth = YearMonth.from(endDate);

            while (!currentMonth.isAfter(endMonth)) {
                BigDecimal monthExpenses = BigDecimal.ZERO;
                LocalDate monthStart = currentMonth.atDay(1);
                LocalDate monthEnd = currentMonth.atEndOfMonth();

                for (Map.Entry<LocalDate, BigDecimal> entry : dailyExpenses.entrySet()) {
                    if (!entry.getKey().isBefore(monthStart) && !entry.getKey().isAfter(monthEnd)) {
                        monthExpenses = monthExpenses.add(entry.getValue());
                    }
                }

                cumulativeExpenses = cumulativeExpenses.add(monthExpenses);
                BigDecimal monthBudget = monthlyBudgets.getOrDefault(currentMonth, BigDecimal.ZERO);
                cumulativeBudget = cumulativeBudget.add(monthBudget);

                Map<String, Object> point = new HashMap<>();
                point.put("date", currentMonth.toString());
                point.put("expenses", cumulativeExpenses);
                point.put("budget", cumulativeBudget);
                point.put("monthExpenses", monthExpenses);
                point.put("monthBudget", monthBudget);
                series.add(point);

                currentMonth = currentMonth.plusMonths(1);
            }
        } else {

            LocalDate currentDate = startDate;
            YearMonth currentMonth = YearMonth.from(startDate);
            BigDecimal currentMonthBudget = monthlyBudgets.getOrDefault(currentMonth, BigDecimal.ZERO);
            int daysInMonth = currentMonth.lengthOfMonth();
            BigDecimal dailyBudget = currentMonthBudget.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

            while (!currentDate.isAfter(endDate)) {

                if (!YearMonth.from(currentDate).equals(currentMonth)) {
                    currentMonth = YearMonth.from(currentDate);
                    currentMonthBudget = monthlyBudgets.getOrDefault(currentMonth, BigDecimal.ZERO);
                    daysInMonth = currentMonth.lengthOfMonth();
                    dailyBudget = currentMonthBudget.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
                }

                BigDecimal dayExpenses = dailyExpenses.getOrDefault(currentDate, BigDecimal.ZERO);
                cumulativeExpenses = cumulativeExpenses.add(dayExpenses);
                cumulativeBudget = cumulativeBudget.add(dailyBudget);

                Map<String, Object> point = new HashMap<>();
                point.put("date", currentDate.toString());
                point.put("expenses", cumulativeExpenses);
                point.put("budget", cumulativeBudget);
                point.put("dayExpenses", dayExpenses);
                point.put("dayBudget", dailyBudget);
                series.add(point);

                currentDate = currentDate.plusDays(1);
            }
        }

        return series;
    }

    @Override
    public List<Report> getReportsByUser(User user) {
        return reportRepository.findAllByUserOrderByGeneratedAtDesc(user);
    }

    @Override
    public Optional<Report> getReportById(Long id) {
        return reportRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteReport(User user, Long id) {
        reportRepository.deleteByUserAndId(user, id);
    }

    @Override
    public Map<String, Object> getCategoryDetails(Report report) {
        try {
            List<Map<String, Object>> categoryDetails = objectMapper.readValue(
                    report.getCategoryDetailsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );

            Map<String, Object> result = new HashMap<>();
            result.put("categories", categoryDetails);
            result.put("totalBudget", report.getTotalBudget());
            result.put("totalExpenses", report.getTotalExpenses());
            result.put("totalSavings", report.getTotalSavings());
            result.put("globalUsagePercentage", report.getGlobalUsagePercentage());

            return result;
        } catch (Exception e) {
            log.error("Erreur lors de la lecture des détails des catégories", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getTimeSeriesData(Report report) {
        try {
            List<Map<String, Object>> timeSeries = objectMapper.readValue(
                    report.getTimeSeriesJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );

            Map<String, Object> result = new HashMap<>();
            result.put("series", timeSeries);
            result.put("startDate", report.getStartDate());
            result.put("endDate", report.getEndDate());
            result.put("type", report.getType().getLabel());

            return result;
        } catch (Exception e) {
            log.error("Erreur lors de la lecture des données temporelles", e);
            return new HashMap<>();
        }
    }

    @Override
    public byte[] generatePdfReport(Report report) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(30, 30, 30, 30);

            Color blue = new DeviceRgb(52, 73, 94);
            Color red = new DeviceRgb(192, 57, 43);
            Color lightBlue = new DeviceRgb(230, 236, 240);
            Color gray = new DeviceRgb(71, 85, 105);
            Color green = new DeviceRgb(39, 174, 96);

            Paragraph title = new Paragraph("RAPPORT DE DÉPENSES")
                    .setFontSize(22)
                    .setBold()
                    .setFontColor(blue)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(18);
            document.add(title);

            Paragraph meta = new Paragraph()
                    .add(new Text("Type : ").setBold().setFontColor(blue))
                    .add(report.getType().getLabel() + "  ")
                    .add(new Text("Période : ").setBold().setFontColor(blue))
                    .add(report.getStartDate() + " au " + report.getEndDate() + "  ")
                    .add(new Text("Généré le : ").setBold().setFontColor(blue))
                    .add(report.getGeneratedAt().toString())
                    .setFontSize(11)
                    .setFontColor(gray)
                    .setMarginBottom(14);
            document.add(meta);

            Paragraph resume = new Paragraph("RÉSUMÉ")
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(blue)
                    .setMarginTop(10);
            document.add(resume);

            document.add(new Paragraph(String.format("Budget total : %.2f MAD", report.getTotalBudget())).setFontColor(gray));
            document.add(new Paragraph(String.format("Dépenses totales : %.2f MAD", report.getTotalExpenses())).setFontColor(gray));
            document.add(new Paragraph(String.format("Économies : %.2f MAD", report.getTotalSavings()))
                    .setFontColor(report.getTotalSavings().compareTo(BigDecimal.ZERO) < 0 ? red : green));
            if (report.getGlobalUsagePercentage() != null) {
                document.add(new Paragraph(String.format("Utilisation : %.1f%%", report.getGlobalUsagePercentage())).setFontColor(gray));
            }

            Paragraph analyse = new Paragraph("ANALYSE")
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(blue)
                    .setMarginTop(16);
            document.add(analyse);

            document.add(new Paragraph("Catégorie dominante : " +
                    (report.getDominantCategory() != null ? report.getDominantCategory() : "N/A")
                    + " (" + String.format("%.2f MAD",
                    report.getDominantCategoryAmount() != null ? report.getDominantCategoryAmount() : BigDecimal.ZERO) + ")").setFontColor(gray));
            document.add(new Paragraph("Catégories en dépassement : " +
                    (report.getOverBudgetCategoriesCount() != null ? report.getOverBudgetCategoriesCount() : 0)).setFontColor(gray));
            document.add(new Paragraph("Montant total des dépassements : " +
                    String.format("%.2f MAD",
                            report.getTotalOverBudgetAmount() != null ? report.getTotalOverBudgetAmount() : BigDecimal.ZERO)).setFontColor(gray));

            Paragraph detail = new Paragraph("DÉTAIL PAR CATÉGORIE")
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(blue)
                    .setMarginTop(16);
            document.add(detail);

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> categoryDetails = mapper.readValue(
                    report.getCategoryDetailsJson(),
                    mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );

            Table table = new Table(5);
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Catégorie")).setBackgroundColor(lightBlue).setFontColor(blue).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Budget")).setBackgroundColor(lightBlue).setFontColor(blue).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Dépenses")).setBackgroundColor(lightBlue).setFontColor(blue).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Utilisation")).setBackgroundColor(lightBlue).setFontColor(blue).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Dépassement")).setBackgroundColor(lightBlue).setFontColor(blue).setBold());

            for (Map<String, Object> category : categoryDetails) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(category.get("name")))).setFontColor(gray));
                double budget = new BigDecimal(String.valueOf(category.get("budget"))).doubleValue();
                double expenses = new BigDecimal(String.valueOf(category.get("expenses"))).doubleValue();
                double usage = Double.parseDouble(String.valueOf(category.get("usagePercentage")));
                double over = new BigDecimal(String.valueOf(category.get("overBudgetAmount"))).doubleValue();
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", budget))).setFontColor(gray));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", expenses))).setFontColor(gray));
                table.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", usage))).setFontColor(usage > 100 ? red : (usage > 80 ? blue : gray)).setBold());
                table.addCell(new Cell().add(new Paragraph(over > 0 ? String.format("%.2f", over) : "-")).setFontColor(over > 0 ? red : green).setBold());
            }
            document.add(table);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF: " + e.getMessage(), e);
            try {
                return generateSimplePdfReport(report);
            } catch (Exception ex) {
                log.error("Impossible de générer même un PDF simple", ex);
                throw new RuntimeException("Erreur lors de la génération du PDF", ex);
            }
        }
    }


    private byte[] generateSimplePdfReport(Report report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("RAPPORT DE DÉPENSES").setFontSize(20).setBold());
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Type: " + report.getType().getLabel()));
        document.add(new Paragraph("Période: " + report.getStartDate() + " au " + report.getEndDate()));
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("RÉSUMÉ"));
        document.add(new Paragraph("Budget total: " + report.getTotalBudget() + " MAD"));
        document.add(new Paragraph("Dépenses totales: " + report.getTotalExpenses() + " MAD"));
        document.add(new Paragraph("Économies: " + report.getTotalSavings() + " MAD"));

        if (report.getGlobalUsagePercentage() != null) {
            document.add(new Paragraph("Utilisation: " +
                    String.format("%.1f", report.getGlobalUsagePercentage()) + "%"));
        }

        document.close();
        return baos.toByteArray();
    }

    private static class CategoryData {
        String categoryName;
        BigDecimal budget = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal overBudgetAmount = BigDecimal.ZERO;
        Double usagePercentage = 0.0;
        int transactionCount = 0;

        CategoryData(String categoryName) {
            this.categoryName = categoryName;
        }
    }


    @Override
    public byte[] generatePdfReportWithChart(Report report, String base64Chart) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(30, 30, 30, 30);

            
            Color blue = new DeviceRgb(52, 73, 94); 
            Color red = new DeviceRgb(192, 57, 43); 
            Color lightBlue = new DeviceRgb(230, 236, 240); 
            Color gray = new DeviceRgb(71, 85, 105);
            Color green = new DeviceRgb(39, 174, 96); 
          
            Paragraph title = new Paragraph("RAPPORT DE DÉPENSES AVEC GRAPHIQUE")
                    .setFontSize(22)
                    .setBold()
                    .setFontColor(blue)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(18);
            document.add(title);

           
            Paragraph meta = new Paragraph()
                    .add(new Text("Type : ").setBold().setFontColor(blue))
                    .add(report.getType().getLabel() + "  ")
                    .add(new Text("Période : ").setBold().setFontColor(blue))
                    .add(report.getStartDate() + " au " + report.getEndDate() + "  ")
                    .add(new Text("Généré le : ").setBold().setFontColor(blue))
                    .add(report.getGeneratedAt().toString())
                    .setFontSize(11)
                    .setFontColor(gray)
                    .setMarginBottom(14);
            document.add(meta);

            
            Paragraph resume = new Paragraph("RÉSUMÉ")
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(blue)
                    .setMarginTop(10);
            document.add(resume);

            document.add(new Paragraph(String.format("Budget total : %.2f MAD", report.getTotalBudget())).setFontColor(gray));
            document.add(new Paragraph(String.format("Dépenses totales : %.2f MAD", report.getTotalExpenses())).setFontColor(gray));
            document.add(new Paragraph(String.format("Économies : %.2f MAD", report.getTotalSavings()))
                    .setFontColor(report.getTotalSavings().compareTo(BigDecimal.ZERO) < 0 ? red : green));
            if (report.getGlobalUsagePercentage() != null) {
                document.add(new Paragraph(String.format("Utilisation : %.1f%%", report.getGlobalUsagePercentage())).setFontColor(gray));
            }

            
            Paragraph analyse = new Paragraph("ANALYSE")
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(blue)
                    .setMarginTop(16);
            document.add(analyse);

            document.add(new Paragraph("Catégorie dominante : " +
                    (report.getDominantCategory() != null ? report.getDominantCategory() : "N/A")
                    + " (" + String.format("%.2f MAD",
                    report.getDominantCategoryAmount() != null ? report.getDominantCategoryAmount() : BigDecimal.ZERO) + ")").setFontColor(gray));
            document.add(new Paragraph("Catégories en dépassement : " +
                    (report.getOverBudgetCategoriesCount() != null ? report.getOverBudgetCategoriesCount() : 0)).setFontColor(gray));
            document.add(new Paragraph("Montant total des dépassements : " +
                    String.format("%.2f MAD",
                            report.getTotalOverBudgetAmount() != null ? report.getTotalOverBudgetAmount() : BigDecimal.ZERO)).setFontColor(gray));

            
            Paragraph detail = new Paragraph("DÉTAIL PAR CATÉGORIE")
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(blue)
                    .setMarginTop(16);
            document.add(detail);

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> categoryDetails = mapper.readValue(
                    report.getCategoryDetailsJson(),
                    mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );

            Table table = new Table(5);
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Catégorie")).setBackgroundColor(lightBlue).setFontColor(blue).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Budget")).setBackgroundColor(lightBlue).setFontColor(blue).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Dépenses")).setBackgroundColor(lightBlue).setFontColor(blue).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Utilisation")).setBackgroundColor(lightBlue).setFontColor(blue).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Dépassement")).setBackgroundColor(lightBlue).setFontColor(blue).setBold());

            for (Map<String, Object> category : categoryDetails) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(category.get("name")))).setFontColor(gray));
                double budget = new BigDecimal(String.valueOf(category.get("budget"))).doubleValue();
                double expenses = new BigDecimal(String.valueOf(category.get("expenses"))).doubleValue();
                double usage = Double.parseDouble(String.valueOf(category.get("usagePercentage")));
                double over = new BigDecimal(String.valueOf(category.get("overBudgetAmount"))).doubleValue();
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", budget))).setFontColor(gray));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", expenses))).setFontColor(gray));
                table.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", usage))).setFontColor(usage > 100 ? red : (usage > 80 ? blue : gray)).setBold());
                table.addCell(new Cell().add(new Paragraph(over > 0 ? String.format("%.2f", over) : "-")).setFontColor(over > 0 ? red : green).setBold());
            }
            document.add(table);


            if (base64Chart != null && !base64Chart.isEmpty()) {
                Div chartDiv = new Div();
                chartDiv.setKeepTogether(true);
                chartDiv.add(new Paragraph("\n--- GRAPHIQUE ---").setBold());
                base64Chart = base64Chart.replace("data:image/png;base64,", "").trim();
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(base64Chart);
                    com.itextpdf.layout.element.Image chartImage =
                            new com.itextpdf.layout.element.Image(
                                    com.itextpdf.io.image.ImageDataFactory.create(imageBytes));
                    chartImage.setWidth(500);
                    chartImage.setHeight(300);
                    chartImage.setMarginTop(10);
                    chartDiv.add(chartImage);
                } catch (IllegalArgumentException e) {
                    chartDiv.add(new Paragraph("Erreur : image du graphique invalide.").setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED));
                }
                document.add(chartDiv);
            } else {
                document.add(new Paragraph("\n(Aucun graphique transmis)").setItalic());
            }



            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF avec graphique", e);
        }
    }
}