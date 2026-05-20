package com.eleclib.repository;

import com.eleclib.model.ReadingPosition;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReadingPositionRepository {

    private static final String TABLE = "reading_position";
    private final SupabaseClient supabase;

    public Optional<ReadingPosition> findByUserIdAndBookId(Long userId, Long bookId) {
        List<ReadingPosition> list = supabase.get(TABLE, Map.of(
                "user_id", "eq." + userId,
                "book_id", "eq." + bookId
        ), ReadingPosition.class);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public ReadingPosition save(ReadingPosition rp) {
        return supabase.post(TABLE, Map.of(
                "user_id", rp.getUserId(),
                "book_id", rp.getBookId(),
                "last_page", rp.getLastPage(),
                "updated_at", rp.getUpdatedAt() != null ? rp.getUpdatedAt().toString() : java.time.Instant.now().toString()
        ), ReadingPosition.class);
    }

    public void deleteByUserIdAndBookId(Long userId, Long bookId) {
        supabase.deleteComposite(TABLE, Map.of("user_id", userId, "book_id", bookId));
    }

    public List<ReadingPosition> findByUserId(Long userId) {
        return supabase.get(TABLE, Map.of("user_id", "eq." + userId), ReadingPosition.class);
    }
}

