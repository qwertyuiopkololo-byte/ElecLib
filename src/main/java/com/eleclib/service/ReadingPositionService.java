package com.eleclib.service;

import com.eleclib.model.Book;
import com.eleclib.model.ReadingPosition;
import com.eleclib.repository.ReadingPositionRepository;
import com.eleclib.util.ReaderPaging;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReadingPositionService {

    private final ReadingPositionRepository repository;
    private final BookService bookService;
    private final ReadingShelfService readingShelfService;

    public int getLastPage(Long userId, Long bookId) {
        return repository.findByUserIdAndBookId(userId, bookId)
                .map(ReadingPosition::getLastPage)
                .map(p -> Math.max(1, p))
                .orElse(1);
    }

    public void saveLastPage(Long userId, Long bookId, int page) {
        saveLastPage(userId, bookId, page, null);
    }

    public void saveLastPage(Long userId, Long bookId, int page, Integer clientTotalPages) {
        if (page < 1) return;
        Book book = bookService.findById(bookId);
        if (book == null) return;
        Integer totalPages = null;
        if (book.isComic()) {
            if (clientTotalPages != null && clientTotalPages > 0) {
                totalPages = clientTotalPages;
            }
        } else {
            totalPages = ReaderPaging.totalPages(book.getText());
        }
        int savedPage = totalPages == null ? page : Math.min(page, totalPages);
        repository.findByUserIdAndBookId(userId, bookId)
                .ifPresent(r -> repository.deleteByUserIdAndBookId(userId, bookId));
        repository.save(ReadingPosition.builder()
                .userId(userId)
                .bookId(bookId)
                .lastPage(savedPage)
                .updatedAt(Instant.now())
                .build());
        if (totalPages != null) {
            readingShelfService.updateReadingSystemShelf(userId, bookId, savedPage >= totalPages);
        }
    }
}

