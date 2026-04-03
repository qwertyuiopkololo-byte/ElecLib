package com.eleclib.service;

import com.eleclib.model.ReadingPosition;
import com.eleclib.repository.ReadingPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReadingPositionService {

    private final ReadingPositionRepository repository;

    public int getLastPage(Long userId, Long bookId) {
        return repository.findByUserIdAndBookId(userId, bookId)
                .map(ReadingPosition::getLastPage)
                .filter(p -> p != null && p >= 1)
                .orElse(1);
    }

    public void saveLastPage(Long userId, Long bookId, int page) {
        if (page < 1) return;
        repository.findByUserIdAndBookId(userId, bookId)
                .ifPresent(r -> repository.deleteByUserIdAndBookId(userId, bookId));
        repository.save(ReadingPosition.builder()
                .userId(userId)
                .bookId(bookId)
                .lastPage(page)
                .updatedAt(Instant.now())
                .build());
    }
}

