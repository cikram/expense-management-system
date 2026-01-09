
package ma.xproce.gestion_depenses_projet.web;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.Category;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.service.CategoryService;
import ma.xproce.gestion_depenses_projet.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;


import org.springframework.web.bind.annotation.*;

import java.util.List;



@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

        @GetMapping("/list")
        @ResponseBody
            public List<java.util.Map<String, Object>> listCategoriesJson(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
                ma.xproce.gestion_depenses_projet.dao.entities.User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
                List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
                for (Category cat : categoryService.getUserCategories(user)) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", cat.getId());
                    map.put("name", cat.getName());
                    map.put("description", cat.getDescription());
                    result.add(map);
                }
                return result;
        }

    @GetMapping
    public String listCategories(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("categories", categoryService.getUserCategories(user));
        model.addAttribute("category", new Category());
        return "categories";
    }

    @PostMapping("/add")
    public String addCategory(@AuthenticationPrincipal UserDetails userDetails, @ModelAttribute Category category) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        categoryService.createCategory(user, category);
        return "redirect:/categories";
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return "redirect:/categories";
    }

    @PostMapping("/update/{id}")
    public String updateCategory(@PathVariable Long id, @ModelAttribute Category category) {
        categoryService.updateCategory(id, category);
        return "redirect:/categories";
    }

    
}