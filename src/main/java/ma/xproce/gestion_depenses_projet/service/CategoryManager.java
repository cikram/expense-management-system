package ma.xproce.gestion_depenses_projet.service;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.dao.repositories.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryManager implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public Category createCategory(User user, Category category) {
        List<Category> existing = categoryRepository.findAllByUser(user);
        boolean alreadyExists = existing.stream().anyMatch(cat ->
                cat.getName().equalsIgnoreCase(category.getName().trim()));
        if (alreadyExists) {
            return existing.stream()
                    .filter(cat -> cat.getName().equalsIgnoreCase(category.getName().trim()))
                    .findFirst()
                    .get();
        }
        category.setUser(user);
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> getUserCategories(User user) {
        return categoryRepository.findAllByUser(user);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    @Override
    public Optional<Category> getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId);
    }

    @Override
    public Category updateCategory(Long categoryId, Category updatedCategory) {
        Optional<Category> existingCategory = categoryRepository.findById(categoryId);
        if (existingCategory.isPresent()) {
            Category category = existingCategory.get();
            category.setName(updatedCategory.getName());
            category.setDescription(updatedCategory.getDescription());
            return categoryRepository.save(category);
        }
        return null;
    }

    @Override
    public long countAllCategories() {
        return categoryRepository.count();
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }


}

