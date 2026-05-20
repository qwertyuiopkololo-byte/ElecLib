package com.eleclib.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReadingWeekActivityDto {
    String label;
    int count;
}
