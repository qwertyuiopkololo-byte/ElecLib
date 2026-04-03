package com.eleclib.model;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    private Long subscriptionId;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public boolean isActive() {
        return "active".equals(status) && LocalDate.now().isBefore(endDate.plusDays(1));
    }
}
