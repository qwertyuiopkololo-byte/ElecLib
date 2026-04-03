package com.eleclib.service;

import com.eleclib.model.BookMark;
import com.eleclib.repository.BookMarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookMarkService {

    private final BookMarkRepository repository;

    public List<BookMark> getBookmarks(Long userId, Long bookId) {
        return repository.findByUserIdAndBookId(userId, bookId);
    }

    public BookMark addBookmark(Long userId, Long bookId, int pageNumber, String title) {
        if (pageNumber < 1) return null;
        String t = title != null ? title.trim() : "";
        return repository.save(BookMark.builder()
                .userId(userId)
                .bookId(bookId)
                .pageNumber(pageNumber)
                .title(t.isEmpty() ? null : t)
                .createdAt(Instant.now())
                .build());
    }

    public BookMark renameBookmark(Long userId, Long bookId, Long bookmarkId, String title) {
        BookMark bm = repository.findById(bookmarkId);
        if (bm == null || !userId.equals(bm.getUserId())) return null;
        // Надёжно: delete + insert
        repository.deleteById(bookmarkId);
        String t = title != null ? title.trim() : "";
        return repository.save(BookMark.builder()
                .userId(bm.getUserId())
                .bookId(bm.getBookId() != null ? bm.getBookId() : bookId)
                .pageNumber(bm.getPageNumber())
                .title(t.isEmpty() ? null : t)
                .createdAt(bm.getCreatedAt() != null ? bm.getCreatedAt() : Instant.now())
                .build());
    }

    public void deleteBookmark(Long userId, Long bookmarkId) {
        BookMark bm = repository.findById(bookmarkId);
        if (bm != null && userId.equals(bm.getUserId())) repository.deleteById(bookmarkId);
    }
}

