package com.eleclib.repository;

import com.eleclib.model.Subscription;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepository {

    private static final String TABLE = "subscriptions";

    private final SupabaseClient supabase;

    public List<Subscription> findByUser_UserIdOrderByEndDateDesc(Long userId) {
        return supabase.getByFilter(TABLE, "user_id", userId, Subscription.class).stream()
                .sorted(Comparator.comparing(Subscription::getEndDate).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Subscription> findFirstByUser_UserIdAndStatusOrderByEndDateDesc(Long userId, String status) {
        return findByUser_UserIdOrderByEndDateDesc(userId).stream()
                .filter(s -> status.equals(s.getStatus()))
                .findFirst();
    }

    public Subscription save(Subscription sub) {
        Map<String, Object> insert = Map.of(
                "user_id", sub.getUserId(),
                "start_date", sub.getStartDate().toString(),
                "end_date", sub.getEndDate().toString(),
                "status", sub.getStatus()
        );
        return supabase.post(TABLE, insert, Subscription.class);
    }

    public List<Subscription> findAll() {
        return supabase.getAll(TABLE, Subscription.class);
    }
}
