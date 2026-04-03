package com.eleclib.config;

import com.eleclib.model.User;
import com.eleclib.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Configuration
public class WebMvcConfig {

    @ControllerAdvice
    @RequiredArgsConstructor
    public static class GlobalModelAdvice {
        private final UserService userService;

        @ModelAttribute("user")
        public User currentUser() {
            return userService.getCurrentUser();
        }
    }
}
