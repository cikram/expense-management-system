package ma.xproce.gestion_depenses_projet.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.Budget;
import ma.xproce.gestion_depenses_projet.dao.entities.Expense;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.service.BudgetService;
import ma.xproce.gestion_depenses_projet.service.ExpenseService;
import ma.xproce.gestion_depenses_projet.service.LastMonthStatisticsService;
import ma.xproce.gestion_depenses_projet.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class UserHomeController {

    private final ExpenseService expenseService;
    private final BudgetService budgetService;
    private final UserService userService;
    private final LastMonthStatisticsService lastMonthStatisticsService;

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        YearMonth currentMonth = YearMonth.now();
        YearMonth monthToDisplay = determineMonthToDisplay(user, currentMonth);

        List<Expense> monthlyExpenses = expenseService.getExpensesByPeriod(
                user, monthToDisplay.atDay(1), monthToDisplay.atEndOfMonth());

        List<Budget> monthlyBudgets = budgetService.getAllUserBudgets(user).stream()
                .filter(b -> b.getMonth().equals(monthToDisplay))
                .collect(Collectors.toList());

        BigDecimal totalExpenses = monthlyExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBudget = monthlyBudgets.stream()
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal difference = totalBudget.subtract(totalExpenses);

        boolean hasFinancialData =
                totalBudget.compareTo(BigDecimal.ZERO) > 0 || totalExpenses.compareTo(BigDecimal.ZERO) > 0;

        Map<String, BigDecimal> expensesByCategory = monthlyExpenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        model.addAttribute("monthToDisplay", monthToDisplay.toString());
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("totalBudget", totalBudget);
        model.addAttribute("difference", difference);
        model.addAttribute("hasFinancialData", hasFinancialData);
        model.addAttribute("expensesByCategory", expensesByCategory);


        try {
            var lastMonthData = lastMonthStatisticsService.getLastValidMonth(user);
            ObjectMapper mapper = new ObjectMapper();

            if (lastMonthData.isPresent()) {
                Map<String, Object> map = lastMonthData.get();

                model.addAttribute("lastMonthLabelsJson", mapper.writeValueAsString(map.get("labels")));
                model.addAttribute("lastMonthValuesJson", mapper.writeValueAsString(map.get("values")));
                model.addAttribute("lastMonthName", map.get("month") + "/" + map.get("year"));

            } else {
                model.addAttribute("lastMonthLabelsJson", "[]");
                model.addAttribute("lastMonthValuesJson", "[]");
                model.addAttribute("lastMonthName", null);
            }

        } catch (Exception e) {
            model.addAttribute("lastMonthLabelsJson", "[]");
            model.addAttribute("lastMonthValuesJson", "[]");
            model.addAttribute("lastMonthName", null);
        }

        return "home";
    }

    private YearMonth determineMonthToDisplay(User user, YearMonth currentMonth) {

        List<Expense> currentMonthExpenses = expenseService.getExpensesByPeriod(
                user, currentMonth.atDay(1), currentMonth.atEndOfMonth());

        List<Budget> currentMonthBudgets = budgetService.getAllUserBudgets(user)
                .stream()
                .filter(b -> b.getMonth().equals(currentMonth))
                .collect(Collectors.toList());

        if (currentMonthExpenses.isEmpty() && currentMonthBudgets.isEmpty()
                && currentMonth.getMonthValue() > 1) {
            return currentMonth.minusMonths(1);
        }
        return currentMonth;
    }
}
