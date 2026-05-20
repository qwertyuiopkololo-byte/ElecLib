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

    @GetMapping("/stats")
    public String stats(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        ReadingStatsDto stats = readingStatsService.buildStats(user.getUserId());
        model.addAttribute("stats", stats);
        int maxWeekly = stats.getWeeklyActivity().stream().mapToInt(ReadingWeekActivityDto::getCount).max().orElse(0);
        model.addAttribute("maxWeekly", maxWeekly > 0 ? maxWeekly : 1);
        model.addAttribute("pageTitle", "Статистика чтения");
        return "reading/stats";
    }
}
