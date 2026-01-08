package ma.xproce.gestion_depenses_projet.dao.repositories;

import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.Expense;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByUser(User user);
    List<Expense> findByUserAndCategory(User user, Category category);
    List<Expense> findByUserAndDateBetween(User user, LocalDate start, LocalDate end);


    Page<Expense> findAllByUser(User user, Pageable pageable);

    Page<Expense> findByUserAndCategory(User user, Category category, Pageable pageable);

    Page<Expense> findByUserAndDateBetween(User user, LocalDate start, LocalDate end, Pageable pageable);


    Page<Expense> findByUserAndCategoryAndDateBetween(
            User user, Category category, LocalDate start, LocalDate end, Pageable pageable);


    @Query("SELECT e FROM Expense e WHERE e.user = :user " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:startDate IS NULL OR e.date >= :startDate) " +
            "AND (:endDate IS NULL OR e.date <= :endDate)")
    Page<Expense> searchExpenses(@Param("user") User user,
                                 @Param("category") Category category,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 Pageable pageable);


    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.category = :category AND FUNCTION('YEAR', e.date) = :year AND FUNCTION('MONTH', e.date) = :month")
    BigDecimal getTotalSpentByUserAndCategoryAndMonth(@Param("user") User user,
                                                      @Param("category") Category category,
                                                      @Param("year") int year,
                                                      @Param("month") int month);




    @Query("SELECT e.category, SUM(e.amount) FROM Expense e " +
            "WHERE e.user = :user AND e.date BETWEEN :startDate AND :endDate " +
            "GROUP BY e.category")
    List<Object[]> getExpensesByCategoryForPeriod(@Param("user") User user,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT e.date, SUM(e.amount) FROM Expense e " +
            "WHERE e.user = :user AND e.date BETWEEN :startDate AND :endDate " +
            "GROUP BY e.date ORDER BY e.date")
    List<Object[]> getDailyExpensesForPeriod(@Param("user") User user,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);



    @Query("""
           SELECT FUNCTION('YEAR', e.date), FUNCTION('MONTH', e.date), SUM(e.amount)
           FROM Expense e
           WHERE e.user = :user
           GROUP BY FUNCTION('YEAR', e.date), FUNCTION('MONTH', e.date)
           ORDER BY FUNCTION('YEAR', e.date) DESC, FUNCTION('MONTH', e.date) DESC
           """)
    List<Object[]> getAllMonthlyTotals(@Param("user") User user);

    @Query("""
           SELECT FUNCTION('DAY', e.date), SUM(e.amount)
           FROM Expense e
           WHERE e.user = :user
             AND FUNCTION('YEAR', e.date) = :year
             AND FUNCTION('MONTH', e.date) = :month
           GROUP BY FUNCTION('DAY', e.date)
           ORDER BY FUNCTION('DAY', e.date)
           """)
    List<Object[]> getDailyExpenses(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("""
            SELECT e.category as category, SUM(e.amount) as total
            FROM Expense e
            WHERE FUNCTION('YEAR', e.date) = :year AND FUNCTION('MONTH', e.date) = :month
            GROUP BY e.category
            ORDER BY total DESC
            """)
    List<Object[]> findTopCategoriesForMonth(@Param("year") int year,
                                                   @Param("month") int month,
                                                   Pageable pageable);
}