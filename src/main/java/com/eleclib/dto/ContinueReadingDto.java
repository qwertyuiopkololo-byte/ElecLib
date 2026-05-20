package com.eleclib.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContinueReadingDto {
    long bookId;
    String title;
    int lastPage;
    int totalPages;
}
