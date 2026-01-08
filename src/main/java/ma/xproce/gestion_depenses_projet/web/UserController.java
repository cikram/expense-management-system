package ma.xproce.gestion_depenses_projet.web;

import lombok.RequiredArgsConstructor;
import ma.xproce.gestion_depenses_projet.dao.entities.User;
import ma.xproce.gestion_depenses_projet.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

   


    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        long reportCount = userService.countUserReports(user.getId());
        long categoryCount = userService.countUserCategories(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("reportCount", reportCount);
        model.addAttribute("categoryCount", categoryCount);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails, 
                                @ModelAttribute User updatedUser,
                                Model model) {
        try {
            User user = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            updatedUser.setId(user.getId());
            updatedUser.setPassword(user.getPassword());
            userService.updateUser(updatedUser);
            return "redirect:/users/profile?success=true";
        } catch (RuntimeException e) {
            User user = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            long reportCount = userService.countUserReports(user.getId());
            long categoryCount = userService.countUserCategories(user.getId());
            model.addAttribute("user", user);
            model.addAttribute("reportCount", reportCount);
            model.addAttribute("categoryCount", categoryCount);
            model.addAttribute("errorMessage", e.getMessage());
            return "profile";
        }
    }


    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/users";
    }
}