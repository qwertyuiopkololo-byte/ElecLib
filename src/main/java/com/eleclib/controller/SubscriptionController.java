package com.eleclib.controller;

import com.eleclib.model.Subscription;
import com.eleclib.model.User;
import com.eleclib.service.QrCodeService;
import com.eleclib.service.SubscriptionService;
import com.eleclib.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final QrCodeService qrCodeService;

    @GetMapping
    public String page(@RequestParam(required = false) Integer required, Model model) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        boolean hasActive = subscriptionService.hasActiveSubscription(user);
        List<Subscription> subs = subscriptionService.getUserSubscriptions(user.getUserId());
        String qrBase64 = qrCodeService.generatePaymentQrBase64(
                "eleclib://subscribe?user=" + user.getUserId() + "&plan=premium");
        model.addAttribute("hasSubscription", hasActive);
        model.addAttribute("subscriptions", subs);
        model.addAttribute("qrCodeBase64", qrBase64);
        model.addAttribute("required", required != null && required == 1);
        return "subscription/page";
    }

    @PostMapping("/activate")
    public String activateStub(RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        try {
            subscriptionService.createSubscription(user, 1);
            redirectAttributes.addFlashAttribute("message", "Подписка активирована.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Не удалось активировать подписку: " + e.getMessage());
        }
        return "redirect:/subscription";
    }
}
