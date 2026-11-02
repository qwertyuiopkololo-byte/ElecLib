package com.eleclib.service;

import com.eleclib.supabase.SupabaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonthlyGoalServiceTest {
    SupabaseClient supabase;
    MonthlyGoalService service;
    final YearMonth month = YearMonth.of(2026, 11);

    @BeforeEach void setUp() {
        supabase = mock(SupabaseClient.class);
        service = new MonthlyGoalService(supabase);
    }

    @ParameterizedTest @ValueSource(ints = {1, 100})
    void acceptsTargetBoundaries(int target) {
        service.saveTarget(7L, month, target, false);
        verify(supabase).upsert("monthly_reading_goals",
                Map.of("user_id", 7L, "month", "2026-11-01", "target", target), "user_id,month", false);
    }

    @ParameterizedTest @ValueSource(ints = {-1, 0, 101})
    void rejectsInvalidTargetsBeforeWriting(int target) {
        assertThrows(IllegalArgumentException.class, () -> service.saveTarget(7L, month, target, false));
        verifyNoInteractions(supabase);
    }

    @Test void completionUsesDuplicateIgnoringCompositeKey() {
        service.saveFinished(7L, 12L, month, LocalDate.of(2026, 11, 2));
        service.saveFinished(7L, 12L, month, LocalDate.of(2026, 11, 3));
        verify(supabase).upsert("monthly_reading_completions", Map.of("user_id", 7L, "book_id", 12L,
                "month", "2026-11-01", "finished_on", "2026-11-02"), "user_id,month,book_id", true);
        verify(supabase).upsert("monthly_reading_completions", Map.of("user_id", 7L, "book_id", 12L,
                "month", "2026-11-01", "finished_on", "2026-11-03"), "user_id,month,book_id", true);
    }

    @Test void nextMonthUsesDifferentCompletionKey() {
        service.saveFinished(7L, 12L, YearMonth.of(2026, 12), LocalDate.of(2026, 12, 1));
        verify(supabase).upsert(eq("monthly_reading_completions"),
                argThat(data -> "2026-12-01".equals(((Map<?, ?>) data).get("month"))),
                eq("user_id,month,book_id"), eq(true));
    }

    @Test void summaryWithoutTargetHasZeroPercent() throws Exception {
        when(supabase.get(eq("monthly_reading_goals"), anyMap(), eq(MonthlyGoalService.GoalRow.class))).thenReturn(List.of());
        when(supabase.count(eq("monthly_reading_completions"), anyMap())).thenReturn(2L);
        var summary = service.summary(7L);
        assertEquals(0, summary.get("target"));
        assertEquals(2L, summary.get("finished"));
        assertEquals(0L, summary.get("percent"));
    }

    @Test void summaryFiltersByCurrentUserAndSaratovMonth() throws Exception {
        var filters = Map.of("user_id", "eq.7", "month", "eq." + YearMonth.now(ZoneId.of("Europe/Saratov")).atDay(1));
        when(supabase.get("monthly_reading_goals", filters, MonthlyGoalService.GoalRow.class))
                .thenReturn(List.of(new MonthlyGoalService.GoalRow(4)));
        when(supabase.count("monthly_reading_completions", filters)).thenReturn(1L);
        assertEquals(25L, service.summary(7L).get("percent"));
    }

    @Test void percentDoesNotExceedOneHundred() throws Exception {
        when(supabase.get(eq("monthly_reading_goals"), anyMap(), eq(MonthlyGoalService.GoalRow.class)))
                .thenReturn(List.of(new MonthlyGoalService.GoalRow(2)));
        when(supabase.count(eq("monthly_reading_completions"), anyMap())).thenReturn(5L);
        assertEquals(100L, service.summary(7L).get("percent"));
    }

    @Test void storageFailureIsNotReportedAsSuccessfulSave() {
        doThrow(new IllegalStateException("storage unavailable")).when(supabase)
                .upsert(anyString(), any(), anyString(), anyBoolean());
        assertThrows(IllegalStateException.class, () -> service.saveTarget(7L, month, 5, false));
    }
}
