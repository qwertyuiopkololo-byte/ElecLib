package com.eleclib.service;

import com.eleclib.model.BookRating;
import com.eleclib.repository.BookRatingRepository;
import com.eleclib.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BookRatingService {

    private final BookRatingRepository ratingRepository;
    private final BookRepository bookRepository;

    public void saveRating(Long userId, Long bookId, int rating, String review) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Рейтинг от 1 до 5");
        bookRepository.findById(bookId).orElseThrow();
        var existing = ratingRepository.findByUser_UserIdAndBook_BookId(userId, bookId);
        if (existing.isPresent()) {
            Instant createdAt = existing.get().getCreatedAt() != null ? existing.get().getCreatedAt() : Instant.now();
            ratingRepository.deleteByUserIdAndBookId(userId, bookId);
            ratingRepository.save(BookRating.builder()
                    .userId(userId).bookId(bookId)
                    .rating(rating)
                    .review(review != null ? review.trim() : null)
                    .createdAt(createdAt)
                    .build());
        } else {
            ratingRepository.save(BookRating.builder()
                    .userId(userId).bookId(bookId)
                    .rating(rating)
                    .review(review != null ? review.trim() : null)
                    .createdAt(Instant.now())
                    .build());
        }
    }

    public BookRating getRating(Long userId, Long bookId) {
        return ratingRepository.findByUser_UserIdAndBook_BookId(userId, bookId).orElse(null);
    }

    public void deleteRating(Long userId, Long bookId) {
        ratingRepository.deleteByUserIdAndBookId(userId, bookId);
    }
}
