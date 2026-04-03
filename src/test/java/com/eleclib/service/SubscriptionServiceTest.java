package com.eleclib.service;

import com.eleclib.model.Subscription;
import com.eleclib.model.User;
import com.eleclib.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    SubscriptionRepository subscriptionRepository;

    @InjectMocks
    SubscriptionService subscriptionService;

    @Test
    void hasActiveSubscription_returnsFalse_whenUserIsNull() {
        assertFalse(subscriptionService.hasActiveSubscription(null));
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void hasActiveSubscription_returnsTrue_whenRepositoryReturnsActiveNotExpiredSubscription() {
        User user = User.builder().userId(10L).build();
        Subscription sub = Subscription.builder()
                .userId(10L)
                .status("active")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .build();
        when(subscriptionRepository.findFirstByUser_UserIdAndStatusOrderByEndDateDesc(10L, "active"))
                .thenReturn(Optional.of(sub));

        assertTrue(subscriptionService.hasActiveSubscription(user));
    }

    @Test
    void hasActiveSubscription_returnsFalse_whenRepositoryReturnsEmpty() {
        User user = User.builder().userId(10L).build();
        when(subscriptionRepository.findFirstByUser_UserIdAndStatusOrderByEndDateDesc(10L, "active"))
                .thenReturn(Optional.empty());

        assertFalse(subscriptionService.hasActiveSubscription(user));
    }

    @Test
    void createSubscription_savesActiveSubscriptionWithValidDates() {
        User user = User.builder().userId(7L).build();
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.createSubscription(user, 1);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, times(1)).save(captor.capture());
        Subscription saved = captor.getValue();
        assertEquals(7L, saved.getUserId());
        assertEquals("active", saved.getStatus());
        assertNotNull(saved.getStartDate());
        assertNotNull(saved.getEndDate());
        assertTrue(saved.getEndDate().isAfter(saved.getStartDate()));
    }
}

