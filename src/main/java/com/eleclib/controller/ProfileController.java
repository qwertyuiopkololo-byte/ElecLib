package com.eleclib.controller;

import com.eleclib.model.User;
import com.eleclib.service.UserService;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/profile")
public class ProfileController {
    private final UserService userService;
    private final com.eleclib.service.SubscriptionService subscriptionService;
    private final SupabaseClient supabase;
    private final PasswordEncoder passwordEncoder;
    private final Path avatarDirectory = Path.of("data", "avatars");

    @GetMapping
    public String profile(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        model.addAttribute("profile", user);
        model.addAttribute("hasSubscription", subscriptionService.hasActiveSubscription(user));
        model.addAttribute("subscriptions", subscriptionService.getUserSubscriptions(user.getUserId()));
        model.addAttribute("hasAvatar", Files.isRegularFile(avatarPath(user)));
        return "profile";
    }

    @PostMapping("/details")
    public String details(@RequestParam String firstName, @RequestParam String lastName, RedirectAttributes flash) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        firstName = firstName.trim();
        lastName = lastName.trim();
        if (firstName.isEmpty() || lastName.isEmpty() || firstName.length() > 100 || lastName.length() > 100) {
            flash.addFlashAttribute("error", "Укажите имя и фамилию длиной до 100 символов.");
        } else {
            try {
                supabase.patch("users", "user_id", user.getUserId(), Map.of("first_name", firstName, "last_name", lastName));
                flash.addFlashAttribute("success", "Имя и фамилия сохранены.");
            } catch (Exception e) {
                log.warn("Profile update failed for user {}", user.getUserId());
                flash.addFlashAttribute("error", "Не удалось сохранить профиль. Попробуйте ещё раз.");
            }
        }
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String password(@RequestParam String oldPassword, @RequestParam String newPassword,
                           @RequestParam String confirmPassword, RedirectAttributes flash) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            flash.addFlashAttribute("error", "Текущий пароль указан неверно.");
        } else if (newPassword.length() < 8 || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            flash.addFlashAttribute("error", "Новый пароль должен содержать от 8 символов и не более 72 байт.");
        } else if (!newPassword.equals(confirmPassword)) {
            flash.addFlashAttribute("error", "Новый пароль и подтверждение не совпадают.");
        } else if (passwordEncoder.matches(newPassword, user.getPassword())) {
            flash.addFlashAttribute("error", "Новый пароль должен отличаться от текущего.");
        } else {
            try {
                supabase.patch("users", "user_id", user.getUserId(), Map.of("password", passwordEncoder.encode(newPassword)));
                flash.addFlashAttribute("success", "Пароль изменён.");
            } catch (Exception e) {
                log.warn("Password update failed for user {}", user.getUserId());
                flash.addFlashAttribute("error", "Не удалось изменить пароль. Попробуйте ещё раз.");
            }
        }
        return "redirect:/profile";
    }

    private Path avatarPath(User user) { return avatarDirectory.resolve(user.getUserId() + ".png"); }

    @GetMapping("/avatar")
    @ResponseBody
    public ResponseEntity<byte[]> avatar() throws java.io.IOException {
        User user = userService.getCurrentUser();
        if (user == null || !Files.isRegularFile(avatarPath(user))) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .body(Files.readAllBytes(avatarPath(user)));
    }

    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file, RedirectAttributes flash) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        try {
            if (file.isEmpty() || file.getSize() > 1_000_000) throw new IllegalArgumentException();
            BufferedImage image;
            try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException();
                var reader = readers.next();
                try {
                    String format = reader.getFormatName();
                    if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg") && !format.equalsIgnoreCase("jpg")) throw new IllegalArgumentException();
                    reader.setInput(input);
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > 16_000_000) throw new IllegalArgumentException();
                    image = reader.read(0);
                } finally { reader.dispose(); }
            }
            BufferedImage resized = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            var graphics = resized.createGraphics();
            try {
                int size = Math.min(image.getWidth(), image.getHeight());
                graphics.drawImage(image, 0, 0, 256, 256, (image.getWidth()-size)/2, (image.getHeight()-size)/2,
                        (image.getWidth()+size)/2, (image.getHeight()+size)/2, null);
            } finally { graphics.dispose(); }
            Files.createDirectories(avatarDirectory);
            Path temp = Files.createTempFile(avatarDirectory, "avatar-", ".png");
            try {
                ImageIO.write(resized, "png", temp.toFile());
                Files.move(temp, avatarPath(user), StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temp); }
            flash.addFlashAttribute("success", "Аватар сохранён.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Выберите изображение PNG или JPEG до 1 МБ и 16 миллионов пикселей.");
        }
        return "redirect:/profile";
    }

    @PostMapping("/avatar/delete")
    public String deleteAvatar(RedirectAttributes flash) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        try {
            Files.deleteIfExists(avatarPath(user));
            flash.addFlashAttribute("success", "Аватар удалён.");
        } catch (Exception e) { flash.addFlashAttribute("error", "Не удалось удалить аватар."); }
        return "redirect:/profile";
    }
}

