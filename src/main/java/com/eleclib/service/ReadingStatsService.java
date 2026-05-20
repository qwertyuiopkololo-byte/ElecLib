package com.eleclib.service;

import com.eleclib.dto.ReadingStatsDto;
import com.eleclib.dto.ReadingWeekActivityDto;
import com.eleclib.model.Book;
import com.eleclib.model.ReadingPosition;
import com.eleclib.repository.BookMarkRepository;
import com.eleclib.repository.BookRepository;
import com.eleclib.repository.ReadingNoteRepository;
import com.eleclib.repository.ReadingPositionRepository;
import com.eleclib.util.ReaderPaging;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingStatsService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter WEEK_LABEL = DateTimeFormatter.ofPattern("dd.MM");

    private final ReadingPositionRepository readingPositionRepository;
    private final BookRepository bookRepository;
    private final BookMarkRepository bookMarkRepository;
    private final ReadingNoteRepository readingNoteRepository;

    public ReadingStatsDto buildStats(Long userId) {
        List<ReadingPosition> positions = readingPositionRepository.findByUserId(userId);
        Set<Long> bookIds = positions.stream().map(ReadingPosition::getBookId).collect(Collectors.toSet());
        Map<Long, Book> booksById = new HashMap<>();
        for (Long id : bookIds) {
            bookRepository.findById(id).ifPresent(b -> booksById.put(id, b));
        }

        int finished = 0;
        int pageSum = 0;
        for (ReadingPosition p : positions) {
            Book b = booksById.get(p.getBookId());
            if (b == null) {
                continue;
            }
            int total = ReaderPaging.totalPages(b.getText());
            int last = p.getLastPage() != null ? p.getLastPage() : 1;
            pageSum += Math.min(Math.max(last, 1), total);
            if (last >= total) {
                finished++;
            }
        }
        int started = positions.size();
        int inProgress = (int) positions.stream()
                .filter(p -> {
                    Book b = booksById.get(p.getBookId());
                    if (b == null) {
                        return false;
                    }
                    int total = ReaderPaging.totalPages(b.getText());
                    int last = p.getLastPage() != null ? p.getLastPage() : 1;
                    return last < total;
                })
                .count();

        int bookmarks = bookMarkRepository.findByUserId(userId).size();
        int notes = readingNoteRepository.findByUserId(userId).size();

        List<ReadingWeekActivityDto> weekly = buildWeeklyActivity(positions);

        return ReadingStatsDto.builder()
                .booksStarted(started)
                .booksFinished(finished)
                .booksInProgress(inProgress)
                .bookmarksTotal(bookmarks)
                .notesTotal(notes)
                .progressPageSum(pageSum)
                .weeklyActivity(weekly)
                .build();
    }

    private List<ReadingWeekActivityDto> buildWeeklyActivity(List<ReadingPosition> positions) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate mondayThisWeek = today.with(DayOfWeek.MONDAY);
        List<ReadingWeekActivityDto> weeklyActivity = new ArrayList<>();
        for (int i = 7; i >= 0; i--) {
            LocalDate weekStart = mondayThisWeek.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusWeeks(1);
            ZonedDateTime zStart = weekStart.atStartOfDay(ZONE);
            ZonedDateTime zEnd = weekEnd.atStartOfDay(ZONE);
            Instant startI = zStart.toInstant();
            Instant endI = zEnd.toInstant();
            String label = weekStart.format(WEEK_LABEL) + " — " + weekStart.plusDays(6).format(WEEK_LABEL);
            long count = positions.stream()
                    .map(ReadingPosition::getUpdatedAt)
                    .filter(Objects::nonNull)
                    .filter(t -> !t.isBefore(startI) && t.isBefore(endI))
                    .count();
            weeklyActivity.add(ReadingWeekActivityDto.builder().label(label).count((int) count).build());
        }
        return weeklyActivity;
    }
}
