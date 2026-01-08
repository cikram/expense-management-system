package ma.xproce.gestion_depenses_projet;

import ma.xproce.gestion_depenses_projet.dao.entities.*;
import ma.xproce.gestion_depenses_projet.dao.repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@SpringBootApplication
public class GestionDepensesProjetApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionDepensesProjetApplication.class, args);
        System.out.println("Application Gestion de Dépenses démarrée avec succès !");
    }

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               ExpenseRepository expenseRepository,
                               BudgetRepository budgetRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {

            
            User user = userRepository.findByUsername("userdemo").orElse(null);
            if (user == null) {
                user = User.builder()
                        .username("userdemo")
                        .email("userdemo@example.com")
                        .password(passwordEncoder.encode("123456"))
                        .build();
                user = userRepository.save(user);
                System.out.println("Utilisateur 'userdemo' créé avec succès !");
            }

            
            final User finalUser = user;

            
            if (categoryRepository.findAllByUser(finalUser).isEmpty()) {
                Category alimentation = Category.builder()
                        .name("Alimentation")
                        .description("Achats de nourriture, restaurants, cafés")
                        .user(finalUser).build();

                Category transport = Category.builder()
                        .name("Transport")
                        .description("Essence, bus, taxi, train")
                        .user(finalUser).build();

                Category loisirs = Category.builder()
                        .name("Loisirs")
                        .description("Cinéma, sorties, abonnements streaming")
                        .user(finalUser).build();

                Category sante = Category.builder()
                        .name("Santé")
                        .description("Pharmacie, médecin, sport")
                        .user(finalUser).build();

                categoryRepository.saveAll(List.of(alimentation, transport, loisirs, sante));

                System.out.println("Catégories standards insérées !");
            }

            
            List<Category> categories = categoryRepository.findAllByUser(finalUser);
            if (expenseRepository.findAllByUser(finalUser).isEmpty()) {
                Expense e1 = Expense.builder()
                        .amount(new BigDecimal("25.50"))
                        .description("Courses Super U")
                        .date(LocalDate.now().minusDays(3))
                        .category(categories.stream().filter(c -> c.getName().equals("Alimentation")).findFirst().orElse(null))
                        .user(finalUser).build();

                Expense e2 = Expense.builder()
                        .amount(new BigDecimal("12.00"))
                        .description("Cinéma - soirée entre amis")
                        .date(LocalDate.now().minusDays(2))
                        .category(categories.stream().filter(c -> c.getName().equals("Loisirs")).findFirst().orElse(null))
                        .user(finalUser).build();

                Expense e3 = Expense.builder()
                        .amount(new BigDecimal("45.00"))
                        .description("Plein d'essence")
                        .date(LocalDate.now().minusDays(1))
                        .category(categories.stream().filter(c -> c.getName().equals("Transport")).findFirst().orElse(null))
                        .user(finalUser).build();

                Expense e4 = Expense.builder()
                        .amount(new BigDecimal("18.90"))
                        .description("Pharmacie - vitamines")
                        .date(LocalDate.now().minusDays(5))
                        .category(categories.stream().filter(c -> c.getName().equals("Santé")).findFirst().orElse(null))
                        .user(finalUser).build();

                expenseRepository.saveAll(List.of(e1, e2, e3, e4));
                System.out.println("Dépenses de test ajoutées !");
            }

            
            YearMonth currentMonth = YearMonth.now();
            if (budgetRepository.findAllByUser(finalUser).isEmpty()) {
                categories.forEach(cat -> {
                    BigDecimal budgetAmount;
                    switch (cat.getName()) {
                        case "Alimentation" -> budgetAmount = new BigDecimal("300.00");
                        case "Transport" -> budgetAmount = new BigDecimal("150.00");
                        case "Loisirs" -> budgetAmount = new BigDecimal("100.00");
                        case "Santé" -> budgetAmount = new BigDecimal("80.00");
                        default -> budgetAmount = new BigDecimal("200.00");
                    }

                    Budget budget = Budget.builder()
                            .month(currentMonth)
                            .amount(budgetAmount)
                            .category(cat)
                            .user(finalUser)
                            .build();

                    budgetRepository.save(budget);
                });

                System.out.println("Budgets du mois créés !");
            }

            System.out.println("Données de démonstration prêtes !");
        };
    }}