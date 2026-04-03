package com.eleclib.controller;

import com.eleclib.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Неверный логин или пароль");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.register(form.getLogin(), form.getPassword(), form.getFirstName(), form.getLastName());
        } catch (IllegalArgumentException e) {
            result.rejectValue("login", "login.exists", e.getMessage());
            return "register";
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            String msg = "Ошибка Supabase: " + e.getStatusCode();
            if (body != null && !body.isBlank()) {
                msg += ". " + (body.length() > 200 ? body.substring(0, 200) + "..." : body);
            } else {
                msg += ". Убедитесь, что указан ключ service_role и таблица users создана (supabase_schema.sql).";
            }
            result.reject("supabase", msg);
            return "register";
        } catch (Exception e) {
            result.reject("supabase", "Ошибка при сохранении пользователя: " + e.getMessage());
            return "register";
        }
        redirectAttributes.addFlashAttribute("message", "Регистрация успешна. Войдите в систему.");
        return "redirect:/login";
    }

    @lombok.Data
    public static class RegisterForm {
        @NotBlank(message = "Введите логин")
        private String login;
        @NotBlank(message = "Введите пароль")
        private String password;
        @NotBlank(message = "Введите имя")
        private String firstName;
        @NotBlank(message = "Введите фамилию")
        private String lastName;
    }
}
