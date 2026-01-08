package ma.xproce.gestion_depenses_projet.service;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.dao.repositories.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LastMonthStatisticsManager implements LastMonthStatisticsService {

    private final ExpenseRepository expenseRepository;

    private BigDecimal toBig(Object o) {
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    @Override
    public Optional<Map<String, Object>> getLastValidMonth(User user) {

        System.out.println("=== getLastValidMonth CALLED ===");

        List<Object[]> monthlyTotals = expenseRepository.getAllMonthlyTotals(user);

        if (monthlyTotals == null || monthlyTotals.isEmpty()) {
            System.out.println("NO MONTHLY TOTALS");
            return Optional.empty();
        }

        monthlyTotals.forEach(r -> System.out.println("MONTH: " + Arrays.toString(r)));

        Integer lastYear = null;
        Integer lastMonth = null;

        for (Object[] row : monthlyTotals) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            BigDecimal total = toBig(row[2]);

            if (total.compareTo(BigDecimal.ZERO) > 0) {
                lastYear = year;
                lastMonth = month;
                break;
            }
        }

        if (lastYear == null || lastMonth == null) return Optional.empty();

        List<Object[]> daily = expenseRepository.getDailyExpenses(user, lastYear, lastMonth);
        daily.forEach(r -> System.out.println("DAY: " + Arrays.toString(r)));

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (Object[] row : daily) {
            labels.add(String.valueOf(row[0]));
            values.add(toBig(row[1]).doubleValue());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("year", lastYear);
        result.put("month", lastMonth);
        result.put("labels", labels);
        result.put("values", values);

        return Optional.of(result);
    }
}

