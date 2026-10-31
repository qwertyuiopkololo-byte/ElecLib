package com.eleclib.dto;

public record LibraryProgressDto(
        BookCardDto book,
        int lastPage,
        int totalPages,
        int percent
) {}
