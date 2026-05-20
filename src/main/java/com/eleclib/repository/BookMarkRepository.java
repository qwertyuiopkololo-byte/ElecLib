package com.eleclib.repository;

import com.eleclib.model.BookMark;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class BookMarkRepository {

    private static final String TABLE = "book_marks";
    private final SupabaseClient supabase;

    public List<BookMark> findByUserIdAndBookId(Long userId, Long bookId) {
        return supabase.get(TABLE, Map.of("user_id", "eq." + userId, "book_id", "eq." + bookId), BookMark.class);
    }

    public List<BookMark> findByUserId(Long userId) {
        return supabase.get(TABLE, Map.of("user_id", "eq." + userId), BookMark.class);
    }

    public BookMark findById(Long id) {
        List<BookMark> list = supabase.get(TABLE, Map.of("id", "eq." + id), BookMark.class);
        return list.isEmpty() ? null : list.get(0);
    }

    public BookMark save(BookMark bm) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", bm.getUserId());
        body.put("book_id", bm.getBookId());
        body.put("page_number", bm.getPageNumber());
        body.put("created_at", bm.getCreatedAt() != null ? bm.getCreatedAt().toString() : java.time.Instant.now().toString());
        if (bm.getTitle() != null && !bm.getTitle().isBlank()) body.put("title", bm.getTitle().trim());
        return supabase.post(TABLE, body, BookMark.class);
    }

    public void deleteById(Long id) {
        supabase.delete(TABLE, "id", id);
    }
}

