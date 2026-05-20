package com.eleclib.model;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingShelfBook {

    private Long shelfId;
    private Long bookId;
    private Instant addedAt;
}
