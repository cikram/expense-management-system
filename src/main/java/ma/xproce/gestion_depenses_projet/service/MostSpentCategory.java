package ma.xproce.gestion_depenses_projet.service;

import ma.xproce.gestion_depenses_projet.dao.entities.Category;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MostSpentCategory(Category category, BigDecimal totalAmount, YearMonth month) {
}
