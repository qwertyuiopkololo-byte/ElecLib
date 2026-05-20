package com.eleclib.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    /** URL изображения обложки (колонка cover_url в Supabase) */
    private String coverUrl;
    /** text — обычная книга; comic — комикс, PDF на диске сервера */
    private String contentType;

    @JsonIgnore
    public boolean isComic() {
        return CONTENT_TYPE_COMIC.equalsIgnoreCase(contentType);
    }

    public static final String CONTENT_TYPE_COMIC = "comic";
    public static final String CONTENT_TYPE_TEXT = "text";
}
