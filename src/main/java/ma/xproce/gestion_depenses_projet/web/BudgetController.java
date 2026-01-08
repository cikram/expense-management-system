package ma.xproce.gestion_depenses_projet.web;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.Budget;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.service.BudgetService;
import ma.xproce.gestion_depenses_projet.service.CategoryService;
import ma.xproce.gestion_depenses_projet.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@Controller
@RequiredArgsConstructor
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final UserService userService;

    @GetMapping
    public String showBudgets(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("budgets", budgetService.getAllUserBudgets(user));
        model.addAttribute("categories", categoryService.getUserCategories(user));
        model.addAttribute("budget", new Budget());
        return "budgets";
    }

    @PostMapping("/add")
    public String addBudget(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam("categoryId") Long categoryId,
                            @RequestParam("month") String month,
                            @ModelAttribute Budget budget) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        var category = categoryService.getUserCategories(user)
                .stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElseThrow();
        budgetService.setBudget(user, category, YearMonth.parse(month), budget);
        return "redirect:/budgets";
    }

    @PostMapping("/update/{id}")
    public String updateBudget(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Long id,
                               @RequestParam("categoryId") Long categoryId,
                               @RequestParam("month") String month,
                               @ModelAttribute Budget budget) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        var category = categoryService.getUserCategories(user)
                .stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElseThrow();
        budget.setId(id);
        budget.setCategory(category);
        budget.setMonth(java.time.YearMonth.parse(month));
        budgetService.updateBudget(user, budget);
        return "redirect:/budgets";
    }
}