package com.eleclib.repository;

import com.eleclib.model.ReadingNote;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ReadingNoteRepository {

    private static final String TABLE = "reading_notes";
    private final SupabaseClient supabase;

    public List<ReadingNote> findByUserIdAndBookId(Long userId, Long bookId) {
        return supabase.get(TABLE, Map.of("user_id", "eq." + userId, "book_id", "eq." + bookId), ReadingNote.class);
    }

    public List<ReadingNote> findByUserId(Long userId) {
        return supabase.get(TABLE, Map.of("user_id", "eq." + userId), ReadingNote.class);
    }

    public ReadingNote findById(Long id) {
        List<ReadingNote> list = supabase.get(TABLE, Map.of("id", "eq." + id), ReadingNote.class);
        return list.isEmpty() ? null : list.get(0);
    }

    public ReadingNote save(ReadingNote n) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", n.getUserId());
        body.put("book_id", n.getBookId());
        body.put("page_number", n.getPageNumber());
        body.put("quote_text", n.getQuoteText());
        body.put("created_at", n.getCreatedAt() != null ? n.getCreatedAt().toString() : java.time.Instant.now().toString());
        if (n.getTitle() != null && !n.getTitle().isBlank()) body.put("title", n.getTitle().trim());
        return supabase.post(TABLE, body, ReadingNote.class);
    }

    public void deleteById(Long id) {
        supabase.delete(TABLE, "id", id);
    }
}

