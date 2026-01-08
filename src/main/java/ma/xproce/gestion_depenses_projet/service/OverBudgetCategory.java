package ma.xproce.gestion_depenses_projet.service;

import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.User;

import java.math.BigDecimal;
import java.time.YearMonth;

public record OverBudgetCategory(
        User user,
        Category category,
        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        YearMonth month
) {}
