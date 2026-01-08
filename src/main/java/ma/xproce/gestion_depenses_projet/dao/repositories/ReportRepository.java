package ma.xproce.gestion_depenses_projet.dao.repositories;

import ma.xproce.gestion_depenses_projet.dao.entities.Report;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByUserOrderByGeneratedAtDesc(User user);

    @Query("SELECT r FROM Report r WHERE r.user = :user AND r.type = :type " +
            "ORDER BY r.generatedAt DESC")
    List<Report> findByUserAndType(@Param("user") User user,
                                   @Param("type") Report.ReportType type);

    long countByUser(User user);

    @Query("SELECT r FROM Report r WHERE r.user = :user " +
            "AND r.startDate = :startDate AND r.endDate = :endDate")
    Optional<Report> findByUserAndPeriod(@Param("user") User user,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    void deleteByUserAndId(User user, Long id);
}