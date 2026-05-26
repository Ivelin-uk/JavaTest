package com.example.helloworld.controller;

import com.example.helloworld.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    private final UserService userService;

    public PageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "index";
    }

    @PostMapping("/users")
    public String createUser(
            @RequestParam String username,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.createUser(username);
            redirectAttributes.addFlashAttribute("successMessage", "Потребителят е създаден успешно.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam String username,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.updateUser(id, username);
            redirectAttributes.addFlashAttribute("successMessage", "Потребителят е редактиран успешно.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Потребителят е изтрит успешно.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/";
    }
}
