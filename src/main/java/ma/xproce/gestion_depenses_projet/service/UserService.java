package ma.xproce.gestion_depenses_projet.service;

import ma.xproce.gestion_depenses_projet.dao.entities.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User register(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> getAllUsers();
    void deleteUser(Long id);
    Optional<User> findById(Long id);
    Page<User> getUsersPage(int page, int size);
    User updateUser(User user);
    long countUserReports(Long userId);
    long countUserCategories(Long userId);
}