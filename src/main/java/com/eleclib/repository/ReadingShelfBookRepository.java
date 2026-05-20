package com.eleclib.repository;

import com.eleclib.model.ReadingShelfBook;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ReadingShelfBookRepository {

    private static final String TABLE = "reading_shelf_book";
    private final SupabaseClient supabase;

    public List<ReadingShelfBook> findByShelfId(Long shelfId) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("shelf_id", "eq." + shelfId);
        queryParams.put("order", "added_at.desc");
        return supabase.get(TABLE, queryParams, ReadingShelfBook.class);
    }

    public List<ReadingShelfBook> findByBookId(Long bookId) {
        return supabase.get(TABLE, Map.of("book_id", "eq." + bookId), ReadingShelfBook.class);
    }

    public ReadingShelfBook save(Long shelfId, Long bookId) {
        Map<String, Object> body = new HashMap<>();
        body.put("shelf_id", shelfId);
        body.put("book_id", bookId);
        body.put("added_at", Instant.now().toString());
        return supabase.post(TABLE, body, ReadingShelfBook.class);
    }

    public void delete(Long shelfId, Long bookId) {
        supabase.deleteComposite(TABLE, Map.of("shelf_id", shelfId, "book_id", bookId));
    }
}
