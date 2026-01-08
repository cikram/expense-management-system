package ma.xproce.gestion_depenses_projet.service;

import ma.xproce.gestion_depenses_projet.dao.entities.Budget;
import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.User;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface BudgetService {
    Budget setBudget(User user, Category category, YearMonth month, Budget budget);
    Optional<Budget> getBudget(User user, Category category, YearMonth month);
    List<Budget> getAllUserBudgets(User user);
    Optional<OverBudgetCategory> getTopOverBudgetForCurrentOrPreviousMonth();

    Optional<OverBudgetCategory> getTopOverBudgetForUserCurrentOrPreviousMonth(User user);
    Budget updateBudget(User user, Budget budget);

    void deleteBudget(Long budgetId);
}

