package ma.xproce.gestion_depenses_projet.dao.repositories;


import ma.xproce.gestion_depenses_projet.dao.entities.Budget;
import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByUserAndCategoryAndMonth(User user, Category category, YearMonth month);
    List<Budget> findAllByUser(User user);
    List<Budget> findByUserOrderByMonthDesc(User user);
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.month = :month")
    List<Budget> findByUserAndMonth(@Param("user") User user, @Param("month") YearMonth month);

        @Query("""
                     SELECT b.user, b.category, b.amount, COALESCE(SUM(e.amount), 0) as spent
                     FROM Budget b
                     LEFT JOIN Expense e ON e.user = b.user AND e.category = b.category
                         AND FUNCTION('YEAR', e.date) = :year AND FUNCTION('MONTH', e.date) = :month
                     WHERE b.month = :monthValue
                     GROUP BY b.user, b.category, b.amount
                     HAVING COALESCE(SUM(e.amount), 0) > b.amount
                     ORDER BY (COALESCE(SUM(e.amount), 0) - b.amount) DESC
                     """)
        List<Object[]> findTopOverBudgetForMonth(@Param("monthValue") YearMonth monthValue,
                                                                                         @Param("year") int year,
                                                                                         @Param("month") int month,
                                                                                         Pageable pageable);
    
    @Query("""
                 SELECT b.user, b.category, b.amount, COALESCE(SUM(e.amount), 0) as spent
                 FROM Budget b
                 LEFT JOIN Expense e ON e.user = b.user AND e.category = b.category
                     AND FUNCTION('YEAR', e.date) = :year AND FUNCTION('MONTH', e.date) = :month
                 WHERE b.user = :user AND b.month = :monthValue
                 GROUP BY b.user, b.category, b.amount
                 HAVING COALESCE(SUM(e.amount), 0) > b.amount
                 ORDER BY (COALESCE(SUM(e.amount), 0) - b.amount) DESC
                 """)
    List<Object[]> findTopOverBudgetForUserAndMonth(@Param("user") User user,
                                                    @Param("monthValue") YearMonth monthValue,
                                                    @Param("year") int year,
                                                    @Param("month") int month,
                                                    Pageable pageable);
}

