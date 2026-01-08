package ma.xproce.gestion_depenses_projet.dao.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    @Column(nullable = false)
    private YearMonth month;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
}

