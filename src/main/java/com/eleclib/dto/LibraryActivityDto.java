package com.eleclib.dto;

import java.time.Instant;

public record LibraryActivityDto(
        String type,
        Long activityId,
        Long bookId,
        String bookTitle,
        Integer pageNumber,
        String title,
        String excerpt,
        Instant createdAt
) {}
