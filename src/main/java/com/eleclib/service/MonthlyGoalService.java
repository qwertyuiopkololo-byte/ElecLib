package com.eleclib.service;

import com.eleclib.supabase.SupabaseClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MonthlyGoalService {
    private final SupabaseClient supabase;
    private static final ZoneId ZONE = ZoneId.of("Europe/Saratov");
    private YearMonth month() { return YearMonth.now(ZONE); }
    public void setTarget(Long userId, int target) throws java.io.IOException {
        saveTarget(userId, month(), target, false);
    }
    public void saveTarget(Long userId, YearMonth month, int target, boolean keepExisting) {
        if (target < 1 || target > 100) throw new IllegalArgumentException("Укажите от 1 до 100 книг.");
        supabase.upsert("monthly_reading_goals", Map.of("user_id", userId, "month", month.atDay(1).toString(),
                "target", target), "user_id,month", keepExisting);
    }
    public void recordFinished(Long userId, Long bookId) throws java.io.IOException {
        saveFinished(userId, bookId, month(), LocalDate.now(ZONE));
    }
    public void saveFinished(Long userId, Long bookId, YearMonth month, LocalDate finishedOn) {
        supabase.upsert("monthly_reading_completions", Map.of("user_id", userId, "book_id", bookId,
                "month", month.atDay(1).toString(), "finished_on", finishedOn.toString()),
                "user_id,month,book_id", true);
    }
    public Map<String, Object> summary(Long userId) throws java.io.IOException {
        YearMonth current = month();
        Map<String, String> filters = Map.of("user_id", "eq." + userId, "month", "eq." + current.atDay(1));
        var goals = supabase.get("monthly_reading_goals", filters, GoalRow.class);
        int target = goals.isEmpty() ? 0 : goals.get(0).target();
        long finished = supabase.count("monthly_reading_completions", filters);
        return Map.of("month", current.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru"))),
                "target", target, "finished", finished, "percent", target == 0 ? 0 : Math.min(100, finished * 100 / target));
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoalRow(int target) { }
}
