package ma.xproce.gestion_depenses_projet.service;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.Expense;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.dao.repositories.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseManager implements ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Override
    public Expense addExpense(User user, Expense expense) {
        expense.setUser(user);
        return expenseRepository.save(expense);
    }

    @Override
    public List<Expense> getExpensesByUser(User user) {
        return expenseRepository.findAllByUser(user);
    }

    @Override
    public List<Expense> getExpensesByCategory(User user, Category category) {
        return expenseRepository.findByUserAndCategory(user, category);
    }

    @Override
    public List<Expense> getExpensesByPeriod(User user, LocalDate start, LocalDate end) {
        return expenseRepository.findByUserAndDateBetween(user, start, end);
    }

    @Override
    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }

    @Override
    public Expense updateExpense(Long id, Expense expense) {
        Expense existingExpense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée"));
        existingExpense.setDescription(expense.getDescription());
        existingExpense.setAmount(expense.getAmount());
        existingExpense.setDate(expense.getDate());
        existingExpense.setCategory(expense.getCategory());
        return expenseRepository.save(existingExpense);
    }

    
    @Override
    public Page<Expense> getExpensesByUser(User user, Pageable pageable) {
        return expenseRepository.findAllByUser(user, pageable);
    }

    @Override
    public Page<Expense> searchExpenses(User user, Category category,
                                        LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return expenseRepository.searchExpenses(user, category, startDate, endDate, pageable);
    }

    @Override
    public BigDecimal getTotalSpentByMonthAndCategory(User user, Category category, YearMonth month) {
        return expenseRepository.getTotalSpentByUserAndCategoryAndMonth(
                user, category, month.getYear(), month.getMonthValue()
        );
    }

    @Override
    public Optional<MostSpentCategory> getTopSpendingCategoryForCurrentMonth() {
        YearMonth currentMonth = YearMonth.now();


        Optional<MostSpentCategory> current = getTopCategoryForMonth(currentMonth);
        if (current.isPresent()) {
            return current;
        }

        YearMonth previousMonth = currentMonth.minusMonths(1);
        return getTopCategoryForMonth(previousMonth);
    }

    @Override
    public Optional<MostSpentCategory> getTopSpendingCategoryForUserCurrentMonth(User user) {
        YearMonth currentMonth = YearMonth.now();


        Optional<MostSpentCategory> current = getTopCategoryForUserAndMonth(user, currentMonth);
        if (current.isPresent()) {
            return current;
        }

        YearMonth previousMonth = currentMonth.minusMonths(1);
        return getTopCategoryForUserAndMonth(user, previousMonth);
    }

    private Optional<MostSpentCategory> getTopCategoryForMonth(YearMonth month) {
        List<Object[]> results = expenseRepository.findTopCategoriesForMonth(
                month.getYear(), month.getMonthValue(), PageRequest.of(0, 1));

        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = results.get(0);
        if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
            return Optional.empty();
        }

        Category category = (Category) row[0];
        BigDecimal totalAmount = (BigDecimal) row[1];
        return Optional.of(new MostSpentCategory(category, totalAmount, month));
    }

    private Optional<MostSpentCategory> getTopCategoryForUserAndMonth(User user, YearMonth month) {
        List<Expense> userExpenses = expenseRepository.findAllByUser(user);
        
        if (userExpenses == null || userExpenses.isEmpty()) {
            return Optional.empty();
        }

        var categoryMap = userExpenses.stream()
                .filter(expense -> YearMonth.from(expense.getDate()).equals(month))
                .collect(java.util.stream.Collectors.groupingBy(
                        Expense::getCategory,
                        java.util.stream.Collectors.reducing(
                                BigDecimal.ZERO,
                                Expense::getAmount,
                                BigDecimal::add
                        )
                ));

        if (categoryMap.isEmpty()) {
            return Optional.empty();
        }

        var topCategory = categoryMap.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(entry -> new MostSpentCategory(entry.getKey(), entry.getValue(), month));

        return topCategory;
    }
}