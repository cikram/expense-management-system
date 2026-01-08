package ma.xproce.gestion_depenses_projet.web;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;


    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }


    @GetMapping("/users/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }


    @PostMapping("/users/register")
    public String registerUser(@ModelAttribute("user") User user, Model model) {
        try {
            userService.register(user); 
            model.addAttribute("successMessage", "Compte créé avec succès !");
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", user);
            return "register";
        }
    }
}
