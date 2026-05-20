package com.eleclib.dto;

import com.eleclib.model.ReadingShelf;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReadingShelfSummaryDto {
    ReadingShelf shelf;
    int bookCount;
}
