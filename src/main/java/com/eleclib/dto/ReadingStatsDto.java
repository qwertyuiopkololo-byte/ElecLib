package com.eleclib.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ReadingStatsDto {
    int booksStarted;
    int booksFinished;
    int booksInProgress;
    int bookmarksTotal;
    int notesTotal;
    /** Сумма min(текущая страница, всего страниц) по книгам с прогрессом */
    int progressPageSum;
    List<ReadingWeekActivityDto> weeklyActivity;
}
