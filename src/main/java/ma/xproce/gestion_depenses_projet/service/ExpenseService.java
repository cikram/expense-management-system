package ma.xproce.gestion_depenses_projet.service;

import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.Expense;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ExpenseService {

    Expense addExpense(User user, Expense expense);
    Expense updateExpense(Long id, Expense expense);
    List<Expense> getExpensesByUser(User user);
    List<Expense> getExpensesByCategory(User user, Category category);
    List<Expense> getExpensesByPeriod(User user, LocalDate start, LocalDate end);
    void deleteExpense(Long id);


    Page<Expense> getExpensesByUser(User user, Pageable pageable);
    Page<Expense> searchExpenses(User user, Category category, LocalDate startDate,
                                 LocalDate endDate, Pageable pageable);
    BigDecimal getTotalSpentByMonthAndCategory(User user, Category category, YearMonth month);
    Optional<MostSpentCategory> getTopSpendingCategoryForCurrentMonth();
    

    Optional<MostSpentCategory> getTopSpendingCategoryForUserCurrentMonth(User user);
}
