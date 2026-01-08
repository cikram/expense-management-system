
package ma.xproce.gestion_depenses_projet.web;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.Admin;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.service.AdminService;
import ma.xproce.gestion_depenses_projet.service.BudgetService;
import ma.xproce.gestion_depenses_projet.service.CategoryService;
import ma.xproce.gestion_depenses_projet.service.ExpenseService;
import ma.xproce.gestion_depenses_projet.service.OverBudgetCategory;
import ma.xproce.gestion_depenses_projet.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ExpenseService expenseService;
    private final BudgetService budgetService;


    @GetMapping("/home")
    public String adminHome(Model model) {
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("totalAdmins", adminService.getAllAdmins().size());
        model.addAttribute("totalCategories", categoryService.countAllCategories());

        var topCategory = expenseService.getTopSpendingCategoryForCurrentMonth();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");
        model.addAttribute("topCategoryName",
            topCategory.map(value -> value.category().getName()).orElse("Aucune dépense"));
        model.addAttribute("topCategoryAmount",
            topCategory.map(value -> value.totalAmount()).orElse(BigDecimal.ZERO));
        model.addAttribute("topCategoryMonth",
            topCategory.map(value -> value.month()).map(month -> month.format(monthFormatter)).orElse(""));

        var overBudget = budgetService.getTopOverBudgetForCurrentOrPreviousMonth();
        model.addAttribute("overBudgetCategory",
            overBudget.map(OverBudgetCategory::category).map(c -> c.getName()).orElse("Aucun dépassement"));
        model.addAttribute("overBudgetUser",
            overBudget.map(OverBudgetCategory::user).map(User::getUsername).orElse(""));
        model.addAttribute("overBudgetBudget",
            overBudget.map(OverBudgetCategory::budgetAmount).orElse(BigDecimal.ZERO));
        model.addAttribute("overBudgetSpent",
            overBudget.map(OverBudgetCategory::spentAmount).orElse(BigDecimal.ZERO));
        model.addAttribute("overBudgetMonth",
            overBudget.map(OverBudgetCategory::month).map(month -> month.format(monthFormatter)).orElse(""));
        
        BigDecimal percentage = overBudget.map(ob -> {
            if (ob.budgetAmount().compareTo(BigDecimal.ZERO) > 0) {
                return ob.spentAmount().subtract(ob.budgetAmount())
                    .divide(ob.budgetAmount(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            }
            return BigDecimal.ZERO;
        }).orElse(BigDecimal.ZERO);
        model.addAttribute("overBudgetPercentage", percentage.setScale(1, java.math.RoundingMode.HALF_UP));
        
        var allUsers = userService.getAllUsers();
        model.addAttribute("recentUsers", allUsers.stream().limit(2).toList());
        
        return "admin/admin-home";
    }

    @GetMapping("/overbudget")
    public String overBudgetDetails(Model model) {
        var overBudget = budgetService.getTopOverBudgetForCurrentOrPreviousMonth();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

        model.addAttribute("overBudgetCategory",
            overBudget.map(OverBudgetCategory::category).map(c -> c.getName()).orElse("Aucun dépassement"));
        model.addAttribute("overBudgetUser",
            overBudget.map(OverBudgetCategory::user).map(User::getUsername).orElse("--"));
        model.addAttribute("overBudgetBudget",
            overBudget.map(OverBudgetCategory::budgetAmount).orElse(BigDecimal.ZERO));
        model.addAttribute("overBudgetSpent",
            overBudget.map(OverBudgetCategory::spentAmount).orElse(BigDecimal.ZERO));
        model.addAttribute("overBudgetMonth",
            overBudget.map(OverBudgetCategory::month).map(month -> month.format(monthFormatter)).orElse("--"));

        BigDecimal percentage = overBudget.map(ob -> {
            if (ob.budgetAmount().compareTo(BigDecimal.ZERO) > 0) {
                return ob.spentAmount().subtract(ob.budgetAmount())
                    .divide(ob.budgetAmount(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            }
            return BigDecimal.ZERO;
        }).orElse(BigDecimal.ZERO);
        model.addAttribute("overBudgetPercentage", percentage.setScale(1, java.math.RoundingMode.HALF_UP));

        return "admin/admin-overbudget";
    }




    @GetMapping("/users/{id}")
    public String userDetails(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        

        model.addAttribute("user", user);
        

        long categoryCount = userService.countUserCategories(id);
        model.addAttribute("categoryCount", categoryCount);
        

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");
        var mostUsedCategory = expenseService.getTopSpendingCategoryForUserCurrentMonth(user);
        model.addAttribute("mostUsedCategory",
            mostUsedCategory.map(value -> value.category().getName()).orElse("Aucune dépense"));
        model.addAttribute("mostUsedCategoryAmount",
            mostUsedCategory.map(value -> value.totalAmount()).orElse(BigDecimal.ZERO));
        model.addAttribute("mostUsedCategoryMonth",
            mostUsedCategory.map(value -> value.month()).map(month -> month.format(monthFormatter)).orElse(""));
        

        var overBudgetCategory = budgetService.getTopOverBudgetForUserCurrentOrPreviousMonth(user);
        model.addAttribute("overBudgetCategory",
            overBudgetCategory.map(OverBudgetCategory::category).map(c -> c.getName()).orElse("Aucun dépassement"));
        model.addAttribute("overBudgetBudget",
            overBudgetCategory.map(OverBudgetCategory::budgetAmount).orElse(BigDecimal.ZERO));
        model.addAttribute("overBudgetSpent",
            overBudgetCategory.map(OverBudgetCategory::spentAmount).orElse(BigDecimal.ZERO));
        model.addAttribute("overBudgetMonth",
            overBudgetCategory.map(OverBudgetCategory::month).map(month -> month.format(monthFormatter)).orElse(""));
        
        BigDecimal percentage = overBudgetCategory.map(ob -> {
            if (ob.budgetAmount().compareTo(BigDecimal.ZERO) > 0) {
                return ob.spentAmount().subtract(ob.budgetAmount())
                    .divide(ob.budgetAmount(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            }
            return BigDecimal.ZERO;
        }).orElse(BigDecimal.ZERO);
        model.addAttribute("overBudgetPercentage", percentage.setScale(1, java.math.RoundingMode.HALF_UP));
        
        return "admin/admin-user-details";
    }


    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users?deleted=true";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        model.addAttribute("user", user);
        return "admin/admin-user-edit";
    }

    

    @PostMapping("/users/edit/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user, Model model) {
        try {
            User existingUser = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            existingUser.setUsername(user.getUsername());
            existingUser.setEmail(user.getEmail());


            if (user.getPassword() != null && !user.getPassword().isBlank()) {
                existingUser.setPassword(user.getPassword());
            }

            userService.updateUser(existingUser);
            return "redirect:/admin/users?updated=true";
        } catch (RuntimeException e) {
            User existingUser = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
            model.addAttribute("user", existingUser);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/admin-user-edit";
        }
    }

    @GetMapping("/admins")
    public String listAdmins(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size,
                             Model model) {

        var adminsPage = adminService.getAdminsPage(page, size);

        model.addAttribute("adminsPage", adminsPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", adminsPage.getTotalPages());

        return "admin/admin-admins";
    }






    @GetMapping("/admins/delete/{id}")
    public String deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return "redirect:/admin/admins?deleted=true";
    }


    @GetMapping("/admins/edit/{id}")
    public String editAdminForm(@PathVariable Long id, Model model) {
        Admin admin = adminService.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin introuvable"));
        model.addAttribute("admin", admin);
        return "admin/admin-admin-edit"; 
    }

    @PostMapping("/admins/edit/{id}")
    public String updateAdmin(@PathVariable Long id, @ModelAttribute Admin admin, Model model) {
        try {
            Admin existingAdmin = adminService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin introuvable"));

            existingAdmin.setUsername(admin.getUsername());
            existingAdmin.setEmail(admin.getEmail());
            

            adminService.updateAdmin(existingAdmin);
            return "redirect:/admin/admins?updated=true";
        } catch (RuntimeException e) {
            Admin existingAdmin = adminService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin introuvable"));
            model.addAttribute("admin", existingAdmin);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/admin-admin-edit";
        }
    }


    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "5") int size,
                            Model model) {

        var usersPage = userService.getUsersPage(page, size);

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());

        return "admin/admin-users";
    }

    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/admin-categories";
    }


    @GetMapping("/profile")
    public String adminProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            String username = userDetails.getUsername();
            Admin admin = adminService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Administrateur non trouvé"));
            model.addAttribute("admin", admin);
        }
        return "admin/admin-profile";
    }

    
    

}
