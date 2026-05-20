package com.eleclib.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookCardDto {
    private Long bookId;
    private String title;
    private String authorName;
    private Long authorId;
    private String genreName;
    private Long genreId;
    private String description;
    private String coverUrl;
    private double averageRating;
    private int reviewCount;
    private boolean inFavorites;
    private boolean hasProgress;
    private Integer lastPosition;
    private boolean subscriptionRequired;
}
