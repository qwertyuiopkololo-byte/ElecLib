package com.eleclib.controller;

import com.eleclib.dto.ReadingStatsDto;
import com.eleclib.dto.ReadingWeekActivityDto;
import com.eleclib.model.User;
import com.eleclib.service.ReadingStatsService;
import com.eleclib.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reading")
@RequiredArgsConstructor
public class ReadingStatsController {

    private final UserService userService;
    private final ReadingStatsService readingStatsService;
    private final com.eleclib.service.MonthlyGoalService monthlyGoalService;

    @GetMapping("/stats")
    public String stats(Model model) throws java.io.IOException {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        ReadingStatsDto stats = readingStatsService.buildStats(user.getUserId());
        model.addAttribute("stats", stats);
        try {
            model.addAttribute("goal", monthlyGoalService.summary(user.getUserId()));
        } catch (Exception e) {
            model.addAttribute("goal", java.util.Map.of("month", "", "target", 0, "finished", 0, "percent", 0));
            model.addAttribute("goalError", "Цели Supabase недоступны. Проверьте подключение и выполните SQL-миграцию.");
        }
        int maxWeekly = stats.getWeeklyActivity().stream().mapToInt(ReadingWeekActivityDto::getCount).max().orElse(0);
        model.addAttribute("maxWeekly", maxWeekly > 0 ? maxWeekly : 1);
        model.addAttribute("pageTitle", "Статистика чтения");
        return "reading/stats";
    }

    @org.springframework.web.bind.annotation.PostMapping("/goal")
    public String goal(@org.springframework.web.bind.annotation.RequestParam int target,
                       org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        try {
            monthlyGoalService.setTarget(user.getUserId(), target);
            flash.addFlashAttribute("goalMessage", "Цель на месяц сохранена.");
        } catch (IllegalArgumentException e) { flash.addFlashAttribute("goalError", e.getMessage()); }
        catch (Exception e) { flash.addFlashAttribute("goalError", "Не удалось сохранить цель в Supabase. Попробуйте ещё раз."); }
        return "redirect:/reading/stats";
    }
}
