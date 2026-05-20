package com.eleclib.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingShelf {

    private Long shelfId;
    private Long userId;
    private String name;
    /** {@code want}, {@code reading}, {@code done} или {@code null} для пользовательской полки */
    private String systemKey;
}
