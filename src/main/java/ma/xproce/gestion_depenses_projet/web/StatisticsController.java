package ma.xproce.gestion_depenses_projet.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.Budget;
import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.Expense;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/statistics")
public class StatisticsController {

    private final ExpenseService expenseService;
    private final CategoryService categoryService;
    private final BudgetService budgetService;
    private final UserService userService;
    private final LastMonthStatisticsService lastMonthStatisticsService;




    @GetMapping
    public String showStatistics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String graphType,
            @RequestParam(required = false) Long category1,
            @RequestParam(required = false) Long category2,
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) String singleMonth,
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth,
            Model model
    ) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        List<Category> allCategories = categoryService.getUserCategories(user);
        List<Budget> allBudgets = budgetService.getAllUserBudgets(user);


        if (singleMonth != null) singleMonth = singleMonth.trim().replaceAll("[^0-9\\-]", "");
        if (startMonth != null) startMonth = startMonth.trim().replaceAll("[^0-9\\-]", "");
        if (endMonth != null) endMonth = endMonth.trim().replaceAll("[^0-9\\-]", "");


        LocalDate startDate;
        LocalDate endDate;
        if ("range".equals(periodType) && startMonth != null && endMonth != null) {
            YearMonth ymStart = YearMonth.parse(startMonth);
            YearMonth ymEnd = YearMonth.parse(endMonth);
            startDate = ymStart.atDay(1);
            endDate = ymEnd.atEndOfMonth();
        } else if (singleMonth != null && !singleMonth.isBlank()) {
            YearMonth ym = YearMonth.parse(singleMonth);
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        } else {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }


        List<Expense> expenses = expenseService.getExpensesByPeriod(user, startDate, endDate);


        Map<String, Map<String, Double>> dataMap = new LinkedHashMap<>();
        Map<String, Map<String, Double>> cumulativeMap = new LinkedHashMap<>();
        Map<String, Map<String, Double>> budgetMap = new LinkedHashMap<>();


        if ("single".equals(graphType) && category1 != null && category1 > 0L) {
            Category cat = allCategories.stream()
                    .filter(c -> c.getId().equals(category1))
                    .findFirst().orElse(null);

            if (cat != null) {
                long monthsBetween = startDate.until(endDate).toTotalMonths();


                if ("month".equals(periodType) && singleMonth != null && !singleMonth.isBlank()) {
                    YearMonth ymSelected = YearMonth.parse(singleMonth);

                    Map<LocalDate, Double> daily = expenses.stream()
                            .filter(e -> e.getCategory().getId().equals(category1))
                            .collect(Collectors.groupingBy(
                                    Expense::getDate,
                                    TreeMap::new,
                                    Collectors.summingDouble(e -> e.getAmount().doubleValue())
                            ));

                    Map<String, Double> converted = new LinkedHashMap<>();
                    Map<String, Double> cumulative = new LinkedHashMap<>();

                    double runningTotal = 0;
                    for (Map.Entry<LocalDate, Double> entry : daily.entrySet()) {
                        runningTotal += entry.getValue();
                        String key = entry.getKey().getDayOfMonth() + "/" + entry.getKey().getMonthValue();
                        converted.put(key, entry.getValue());
                        cumulative.put(key, runningTotal);
                    }

                    dataMap.put(cat.getName(), converted);
                    cumulativeMap.put(cat.getName(), cumulative);


                    Map<String, Double> budgetDaily = allBudgets.stream()
                            .filter(b -> b.getCategory().getId().equals(category1)
                                    && b.getMonth().equals(ymSelected))
                            .collect(Collectors.toMap(
                                    b -> "Budget",
                                    b -> b.getAmount().doubleValue(),
                                    Double::sum
                            ));
                    budgetMap.put(cat.getName(), budgetDaily);
                    model.addAttribute("timeScale", "day");

                }

                else if (monthsBetween > 12) {
                    Map<Year, Double> yearly = expenses.stream()
                            .filter(e -> e.getCategory().getId().equals(category1))
                            .collect(Collectors.groupingBy(
                                    e -> Year.from(e.getDate()),
                                    TreeMap::new,
                                    Collectors.summingDouble(e -> e.getAmount().doubleValue())
                            ));
                    Map<String, Double> converted = new LinkedHashMap<>();
                    yearly.forEach((y, val) -> converted.put(y.toString(), val));
                    dataMap.put(cat.getName(), converted);

                    Map<String, Double> budgetYearly = allBudgets.stream()
                            .filter(b -> b.getCategory().getId().equals(category1))
                            .collect(Collectors.groupingBy(
                                    b -> String.valueOf(b.getMonth().getYear()),
                                    Collectors.summingDouble(b -> b.getAmount().doubleValue())
                            ));
                    budgetMap.put(cat.getName(), budgetYearly);
                    model.addAttribute("timeScale", "year");
                }

                else {
                    Map<YearMonth, Double> monthly = expenses.stream()
                            .filter(e -> e.getCategory().getId().equals(category1))
                            .collect(Collectors.groupingBy(
                                    e -> YearMonth.from(e.getDate()),
                                    TreeMap::new,
                                    Collectors.summingDouble(e -> e.getAmount().doubleValue())
                            ));
                    Map<String, Double> converted = new LinkedHashMap<>();
                    monthly.forEach((ym, val) -> converted.put(ym.toString(), val));
                    dataMap.put(cat.getName(), converted);

                    Map<String, Double> budgetMonthly = allBudgets.stream()
                            .filter(b -> b.getCategory().getId().equals(category1))
                            .collect(Collectors.toMap(
                                    b -> b.getMonth().toString(),
                                    b -> b.getAmount().doubleValue(),
                                    Double::sum));
                    budgetMap.put(cat.getName(), budgetMonthly);
                    model.addAttribute("timeScale", "month");
                }

                model.addAttribute("chartKind", "line");
            }
        }


        else if ("compare".equals(graphType) && category1 != null) {


            if (category1 == -1L) {
                Map<String, Double> totals = expenses.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getCategory().getName(),
                                LinkedHashMap::new,
                                Collectors.summingDouble(e -> e.getAmount().doubleValue())
                        ));
                dataMap.put("ALL", totals);
                model.addAttribute("chartKind", "pie");
            }


            else {
                List<Long> ids = new ArrayList<>();
                ids.add(category1);
                if (category2 != null && category2 != 0L)
                    ids.add(category2);

                for (Long id : ids) {
                    Category cat = allCategories.stream()
                            .filter(c -> c.getId().equals(id))
                            .findFirst().orElse(null);
                    if (cat == null) continue;

                    Map<YearMonth, Double> monthly = expenses.stream()
                            .filter(e -> e.getCategory().getId().equals(id))
                            .collect(Collectors.groupingBy(
                                    e -> YearMonth.from(e.getDate()),
                                    TreeMap::new,
                                    Collectors.summingDouble(e -> e.getAmount().doubleValue())
                            ));
                    Map<String, Double> converted = new LinkedHashMap<>();
                    monthly.forEach((ym, val) -> converted.put(ym.toString(), val));
                    dataMap.put(cat.getName(), converted);

                    Map<String, Double> budgetValues = allBudgets.stream()
                            .filter(b -> b.getCategory().getId().equals(id))
                            .collect(Collectors.toMap(
                                    b -> b.getMonth().toString(),
                                    b -> b.getAmount().doubleValue(),
                                    Double::sum));
                    budgetMap.put(cat.getName(), budgetValues);
                }
                model.addAttribute("chartKind", "line");
            }
        }


        else {
            Map<String, Double> totals = expenses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getCategory().getName(),
                            LinkedHashMap::new,
                            Collectors.summingDouble(e -> e.getAmount().doubleValue())
                    ));
            dataMap.put("ALL", totals);
            model.addAttribute("chartKind", "pie");
        }


        model.addAttribute("categories", allCategories);
        model.addAttribute("expensesEvolution", dataMap);
        model.addAttribute("cumulativeEvolution", cumulativeMap);
        model.addAttribute("budgetsEvolution", budgetMap);

        ObjectMapper mapper = new ObjectMapper();
        try {
            model.addAttribute("expensesEvolutionJson", mapper.writeValueAsString(dataMap));
            model.addAttribute("cumulativeEvolutionJson", mapper.writeValueAsString(cumulativeMap));
            model.addAttribute("budgetsEvolutionJson", mapper.writeValueAsString(budgetMap));
        } catch (Exception e) {
            model.addAttribute("expensesEvolutionJson", "{}");
            model.addAttribute("cumulativeEvolutionJson", "{}");
            model.addAttribute("budgetsEvolutionJson", "{}");
        }


        return "statistics";
    }
}