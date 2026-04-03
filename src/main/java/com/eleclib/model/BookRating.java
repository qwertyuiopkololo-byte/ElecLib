package com.eleclib.model;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookRating {

    private Long userId;
    private Long bookId;
    private Integer rating;
    private String review;
    private Instant createdAt;
}
