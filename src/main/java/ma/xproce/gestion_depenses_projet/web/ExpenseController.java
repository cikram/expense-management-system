package ma.xproce.gestion_depenses_projet.web;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.Expense;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.service.BudgetService;
import ma.xproce.gestion_depenses_projet.service.ExpenseService;
import ma.xproce.gestion_depenses_projet.service.CategoryService;
import ma.xproce.gestion_depenses_projet.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

@Controller
@RequiredArgsConstructor
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final BudgetService BudgetService;

    @GetMapping
    public String showExpenses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));


        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));


        Category category = null;
        if (categoryId != null) {
            category = categoryService.getCategoryById(categoryId)
                    .orElse(null);
        }


        Page<Expense> expensePage = expenseService.searchExpenses(
                user, category, startDate, endDate, pageable);


        model.addAttribute("expenses", expensePage.getContent());
        model.addAttribute("expensePage", expensePage);
        model.addAttribute("categories", categoryService.getUserCategories(user));
        model.addAttribute("expense", new Expense());


        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", expensePage.getTotalPages());
        model.addAttribute("totalElements", expensePage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDirection", sortDirection);


        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "expenses";
    }


    @PostMapping("/add")
    public String addExpense(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute Expense expense,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String newCategoryName,
            @RequestParam(required = false) String newCategoryDescription,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "false") boolean confirmOverBudget,
            Model model) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));


        Category category;
        if (newCategoryName != null && !newCategoryName.trim().isEmpty()) {
            Category newCategory = new Category();
            newCategory.setName(newCategoryName.trim());
            newCategory.setDescription(newCategoryDescription);
            newCategory.setUser(user);
            category = categoryService.createCategory(user, newCategory);
        } else if (categoryId != null && !categoryId.equals("NEW_CATEGORY") && !categoryId.isEmpty()) {
            try {
                Long catId = Long.parseLong(categoryId);
                category = categoryService.getCategoryById(catId)
                        .orElseThrow(() -> new RuntimeException("Category not found"));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid category id");
            }
        } else if (categoryId != null && categoryId.equals("NEW_CATEGORY")) {
            throw new RuntimeException("Le nom de la nouvelle catégorie doit être renseigné.");
        } else {
            throw new RuntimeException("Category must be specified");
        }

        expense.setUser(user);
        expense.setCategory(category);


        YearMonth month = YearMonth.from(expense.getDate());
        var budgetOpt = BudgetService.getBudget(user, category, month);

        if (budgetOpt.isPresent()) {
            var budget = budgetOpt.get();
            var totalSpent = expenseService.getTotalSpentByMonthAndCategory(user, category, month);
            var totalAfter = totalSpent.add(expense.getAmount());


            if (totalAfter.compareTo(budget.getAmount()) > 0 && !confirmOverBudget) {
                model.addAttribute("showOverBudgetModal", true);
                model.addAttribute("budgetAmount", budget.getAmount());
                model.addAttribute("totalSpent", totalSpent);
                model.addAttribute("proposedExpense", expense);
                model.addAttribute("categories", categoryService.getUserCategories(user));
                model.addAttribute("expense", new Expense());
                return "expenses";
            }
        }


        if (confirmOverBudget) {
            expense.setOverBudget(true);
        }

        expenseService.addExpense(user, expense);
        return String.format("redirect:/expenses?page=%d&size=%d", page, size);
    }


    @PostMapping("/update/{id}")
    public String updateExpense(
            @PathVariable Long id,
            @ModelAttribute Expense expense,
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        expense.setCategory(category);
        
        expenseService.updateExpense(id, expense);
        return String.format("redirect:/expenses?page=%d&size=%d", page, size);
    }

    @PostMapping("/delete/{id}")
    public String deleteExpense(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        expenseService.deleteExpense(id);
        return String.format("redirect:/expenses?page=%d&size=%d", page, size);
    }

    @GetMapping("/search")
    public String searchExpenses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {


        return String.format("redirect:/expenses?page=%d&size=%d&sortBy=%s&sortDirection=%s&categoryId=%s&startDate=%s&endDate=%s",
                page, size, sortBy, sortDirection,
                categoryId != null ? categoryId.toString() : "",
                startDate != null ? startDate.toString() : "",
                endDate != null ? endDate.toString() : "");
    }


    @GetMapping("/page")
    @ResponseBody
    public Page<Expense> getExpensesPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Category category = categoryId != null
                ? categoryService.getCategoryById(categoryId).orElse(null)
                : null;

        return expenseService.searchExpenses(user, category, startDate, endDate, pageable);
    }

}
