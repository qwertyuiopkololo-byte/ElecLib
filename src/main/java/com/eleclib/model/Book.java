package com.eleclib.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    private Long bookId;
    private String title;
    private String description;
    private String text;
    private Long authorId;
    private Long genreId;
}
