package com.insurance.autoinsurance.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {
        if (error  != null) model.addAttribute("loginError",  true);
        if (logout != null) model.addAttribute("logoutMsg",   true);
        return "auth/login";
    }
}
