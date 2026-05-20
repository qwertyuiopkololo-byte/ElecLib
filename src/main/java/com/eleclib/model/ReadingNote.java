package com.eleclib.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingNote {
    @JsonAlias({"note_id", "reading_note_id"})
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer pageNumber;
    private String quoteText;
    private String title;
    private Instant createdAt;
}

