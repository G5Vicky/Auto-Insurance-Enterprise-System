package com.insurance.autoinsurance.controller;
import com.insurance.autoinsurance.model.AppUser;
import com.insurance.autoinsurance.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AppUserRepository userRepo;
    private final PasswordEncoder   enc;

    public AdminController(AppUserRepository userRepo, PasswordEncoder enc) {
        this.userRepo = userRepo; this.enc = enc;
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users",      userRepo.findAll());
        model.addAttribute("activePage", "admin");
        return "admin/users";
    }

    @GetMapping("/users/add")
    public String addForm(Model model) {
        model.addAttribute("roles",      AppUser.Role.values());
        model.addAttribute("activePage", "admin");
        return "admin/userform";
    }

    @PostMapping("/users/add")
    public String addSubmit(@RequestParam String username,
                            @RequestParam String password,
                            @RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam AppUser.Role role,
                            RedirectAttributes flash) {
        if (userRepo.existsByUsername(username)) {
            flash.addFlashAttribute("errorMsg", "Username '" + username + "' already exists.");
            return "redirect:/admin/users/add";
        }
        userRepo.save(AppUser.builder()
            .username(username).passwordHash(enc.encode(password))
            .fullName(fullName).email(email).role(role).enabled(true).build());
        flash.addFlashAttribute("successMsg", "User '" + username + "' created.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes flash) {
        AppUser u = userRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        u.setEnabled(!u.isEnabled());
        userRepo.save(u);
        flash.addFlashAttribute("successMsg",
            "User '" + u.getUsername() + "' " + (u.isEnabled() ? "enabled" : "disabled") + ".");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/resetpw")
    public String resetPw(@PathVariable Long id, @RequestParam String newPassword,
                          RedirectAttributes flash) {
        AppUser u = userRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        u.setPasswordHash(enc.encode(newPassword));
        userRepo.save(u);
        flash.addFlashAttribute("successMsg", "Password reset for '" + u.getUsername() + "'.");
        return "redirect:/admin/users";
    }
}
