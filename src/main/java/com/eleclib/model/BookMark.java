package com.eleclib.model;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookMark {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer pageNumber;
    private String title;
    private Instant createdAt;
}

