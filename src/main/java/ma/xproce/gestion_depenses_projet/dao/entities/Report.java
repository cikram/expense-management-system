package ma.xproce.gestion_depenses_projet.dao.entities;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ReportType type;

    private LocalDateTime generatedAt;

    private LocalDate startDate;
    private LocalDate endDate;


    private BigDecimal totalBudget;
    private BigDecimal totalExpenses;
    private BigDecimal totalSavings;
    private Double globalUsagePercentage;


    private String dominantCategory;
    private BigDecimal dominantCategoryAmount;


    private Integer overBudgetCategoriesCount;
    private BigDecimal totalOverBudgetAmount;


    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String categoryDetailsJson;


    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String timeSeriesJson;



    public enum ReportType {
        MONTHLY("Mensuel"),
        ANNUAL("Annuel"),
        CUSTOM("Personnalisé");

        private final String label;

        ReportType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}