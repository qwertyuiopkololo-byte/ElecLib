package com.eleclib.service;

import com.eleclib.model.Subscription;
import com.eleclib.model.User;
import com.eleclib.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public boolean hasActiveSubscription(User user) {
        if (user == null) return false;
        Optional<Subscription> sub = subscriptionRepository
                .findFirstByUser_UserIdAndStatusOrderByEndDateDesc(user.getUserId(), "active");
        return sub.map(Subscription::isActive).orElse(false);
    }

    public List<Subscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUser_UserIdOrderByEndDateDesc(userId);
    }

    public Subscription createSubscription(User user, int months) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(months);
        Subscription sub = Subscription.builder()
                .userId(user.getUserId())
                .startDate(start)
                .endDate(end)
                .status("active")
                .build();
        return subscriptionRepository.save(sub);
    }
}
