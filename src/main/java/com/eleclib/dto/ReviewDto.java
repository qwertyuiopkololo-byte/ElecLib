package com.eleclib.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReviewDto {
    private String userFullName;
    private Integer rating;
    private String review;
    private Instant createdAt;
    private boolean isCurrentUser;
}
