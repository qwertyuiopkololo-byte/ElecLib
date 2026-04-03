package com.eleclib.service;

import com.eleclib.model.User;
import com.eleclib.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getCurrentUser() {
        try {
            String name = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getName()
                    : null;
            if (name == null || "anonymousUser".equals(name)) return null;
            return userRepository.findByLogin(name).orElse(null);
        } catch (Exception e) {
            log.warn("Не удалось загрузить текущего пользователя: {}", e.getMessage());
            return null;
        }
    }

    public User findByLogin(String login) {
        return userRepository.findByLogin(login).orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User register(String login, String rawPassword, String firstName, String lastName) {
        if (userRepository.existsByLogin(login)) {
            throw new IllegalArgumentException("Логин уже занят");
        }
        User user = User.builder()
                .login(login)
                .password(passwordEncoder.encode(rawPassword))
                .firstName(firstName)
                .lastName(lastName)
                .role("app_user")
                .build();
        return userRepository.save(user);
    }
}
