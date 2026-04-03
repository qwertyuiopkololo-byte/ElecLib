package com.eleclib.repository;

import com.eleclib.model.BookRating;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookRatingRepository {

    private static final String TABLE = "book_ratings";

    private final SupabaseClient supabase;

    public List<BookRating> findByBook_BookId(Long bookId) {
        return supabase.getByFilter(TABLE, "book_id", bookId, BookRating.class);
    }

    public Optional<BookRating> findByUser_UserIdAndBook_BookId(Long userId, Long bookId) {
        List<BookRating> list = supabase.get(TABLE, Map.of("user_id", "eq." + userId, "book_id", "eq." + bookId), BookRating.class);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Double getAverageRatingByBookId(Long bookId) {
        List<BookRating> list = findByBook_BookId(bookId);
        if (list.isEmpty()) return null;
        return list.stream().mapToInt(BookRating::getRating).average().orElse(0);
    }

    public BookRating save(BookRating br) {
        java.util.Map<String, Object> insert = new java.util.LinkedHashMap<>();
        insert.put("user_id", br.getUserId());
        insert.put("book_id", br.getBookId());
        insert.put("rating", br.getRating());
        insert.put("review", br.getReview() != null ? br.getReview() : "");
        insert.put("created_at", br.getCreatedAt() != null ? br.getCreatedAt().toString() : java.time.Instant.now().toString());
        return supabase.post(TABLE, insert, BookRating.class);
    }

    public void patch(BookRating br) {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("rating", br.getRating());
        body.put("review", br.getReview() != null ? br.getReview() : "");
        body.put("created_at", br.getCreatedAt() != null ? br.getCreatedAt().toString() : java.time.Instant.now().toString());
        supabase.patchComposite(TABLE, Map.of("user_id", br.getUserId(), "book_id", br.getBookId()), body);
    }

    public void deleteByUserIdAndBookId(Long userId, Long bookId) {
        supabase.deleteComposite(TABLE, Map.of("user_id", userId, "book_id", bookId));
    }
}
