package com.eleclib.dto;

import java.util.List;

public record CatalogPageDto(List<BookCardDto> books, int page, boolean hasNext) {
}
