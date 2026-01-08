package ma.xproce.gestion_depenses_projet.service;

import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.User;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    Category createCategory(User user, Category category);
    List<Category> getUserCategories(User user);
    void deleteCategory(Long categoryId);
    Optional<Category> getCategoryById(Long id);
    Category updateCategory(Long categoryId, Category updatedCategory);
    long countAllCategories();
    List<Category> getAllCategories();
}

