package com.eleclib.model;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingPosition {
    private Long userId;
    private Long bookId;
    private Integer lastPage;
    private Instant updatedAt;
}

