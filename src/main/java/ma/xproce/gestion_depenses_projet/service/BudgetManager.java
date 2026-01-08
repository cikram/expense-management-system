package ma.xproce.gestion_depenses_projet.service;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.Budget;
import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.dao.repositories.BudgetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BudgetManager implements BudgetService {

    private final BudgetRepository budgetRepository;

    @Override
    public Budget setBudget(User user, Category category, YearMonth month, Budget budget) {
        Optional<Budget> existing = budgetRepository.findByUserAndCategoryAndMonth(user, category, month);
        if (existing.isPresent()) {
            throw new IllegalStateException("Budget already exists for this month and category!");
        }
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonth(month);
        return budgetRepository.save(budget);
    }

    @Override
    public Optional<Budget> getBudget(User user, Category category, YearMonth month) {
        return budgetRepository.findByUserAndCategoryAndMonth(user, category, month);
    }

    @Override
    public List<Budget> getAllUserBudgets(User user) {
        return budgetRepository.findAllByUser(user);
    }

    @Override
    public Optional<OverBudgetCategory> getTopOverBudgetForCurrentOrPreviousMonth() {
        YearMonth current = YearMonth.now();

        Optional<OverBudgetCategory> currentResult = getTopOverBudgetForMonth(current);
        if (currentResult.isPresent()) {
            return currentResult;
        }

        YearMonth previous = current.minusMonths(1);
        return getTopOverBudgetForMonth(previous);
    }

    @Override
    public Optional<OverBudgetCategory> getTopOverBudgetForUserCurrentOrPreviousMonth(User user) {
        YearMonth current = YearMonth.now();

        Optional<OverBudgetCategory> currentResult = getTopOverBudgetForUserAndMonth(user, current);
        if (currentResult.isPresent()) {
            return currentResult;
        }

        YearMonth previous = current.minusMonths(1);
        return getTopOverBudgetForUserAndMonth(user, previous);
    }

    private Optional<OverBudgetCategory> getTopOverBudgetForMonth(YearMonth month) {
        List<Object[]> rows = budgetRepository.findTopOverBudgetForMonth(
                month, month.getYear(), month.getMonthValue(), PageRequest.of(0, 1));

        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = rows.get(0);
        if (row == null || row.length < 4 || row[0] == null || row[1] == null || row[2] == null || row[3] == null) {
            return Optional.empty();
        }

        User user = (User) row[0];
        Category category = (Category) row[1];
        var budgetAmount = (java.math.BigDecimal) row[2];
        var spentAmount = (java.math.BigDecimal) row[3];

        return Optional.of(new OverBudgetCategory(user, category, budgetAmount, spentAmount, month));
    }

    private Optional<OverBudgetCategory> getTopOverBudgetForUserAndMonth(User user, YearMonth month) {
        List<Object[]> rows = budgetRepository.findTopOverBudgetForUserAndMonth(
                user, month, month.getYear(), month.getMonthValue(), PageRequest.of(0, 1));

        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = rows.get(0);
        if (row == null || row.length < 4 || row[0] == null || row[1] == null || row[2] == null || row[3] == null) {
            return Optional.empty();
        }

        User resultUser = (User) row[0];
        Category category = (Category) row[1];
        var budgetAmount = (java.math.BigDecimal) row[2];
        var spentAmount = (java.math.BigDecimal) row[3];

        return Optional.of(new OverBudgetCategory(resultUser, category, budgetAmount, spentAmount, month));
    }

    @Override
    public Budget updateBudget(User user, Budget budget) {
        budget.setUser(user);
        return budgetRepository.save(budget);
    }

    @Override
    public void deleteBudget(Long budgetId) {
        budgetRepository.deleteById(budgetId);
    }
}

