package com.eleclib.service;

import com.eleclib.model.ReadingNote;
import com.eleclib.repository.ReadingNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingNoteService {
    private final ReadingNoteRepository repository;

    public List<ReadingNote> getNotes(Long userId, Long bookId) {
        return repository.findByUserIdAndBookId(userId, bookId);
    }

    public ReadingNote addNote(Long userId, Long bookId, int pageNumber, String quoteText, String title) {
        if (pageNumber < 1) return null;
        String q = quoteText != null ? quoteText.trim() : "";
        if (q.isEmpty()) return null;
        String t = title != null ? title.trim() : "";
        return repository.save(ReadingNote.builder()
                .userId(userId)
                .bookId(bookId)
                .pageNumber(pageNumber)
                .quoteText(q)
                .title(t.isEmpty() ? null : t)
                .createdAt(Instant.now())
                .build());
    }

    public ReadingNote renameNote(Long userId, Long bookId, Long noteId, String title) {
        ReadingNote n = repository.findById(noteId);
        if (n == null || !userId.equals(n.getUserId())) return null;
        repository.deleteById(noteId);
        String t = title != null ? title.trim() : "";
        return repository.save(ReadingNote.builder()
                .userId(n.getUserId())
                .bookId(n.getBookId() != null ? n.getBookId() : bookId)
                .pageNumber(n.getPageNumber())
                .quoteText(n.getQuoteText())
                .title(t.isEmpty() ? null : t)
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt() : Instant.now())
                .build());
    }

    public void deleteNote(Long userId, Long noteId) {
        ReadingNote n = repository.findById(noteId);
        if (n != null && userId.equals(n.getUserId())) repository.deleteById(noteId);
    }
}

