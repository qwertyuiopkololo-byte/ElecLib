package com.eleclib.repository;

import com.eleclib.model.ReadingShelf;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ReadingShelfRepository {

    private static final String TABLE = "reading_shelf";
    private final SupabaseClient supabase;

    public List<ReadingShelf> findByUserId(Long userId) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("user_id", "eq." + userId);
        queryParams.put("order", "shelf_id.asc");
        return supabase.get(TABLE, queryParams, ReadingShelf.class);
    }

    public ReadingShelf findById(Long shelfId) {
        return supabase.getById(TABLE, "shelf_id", shelfId, ReadingShelf.class);
    }

    public ReadingShelf save(ReadingShelf shelf) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", shelf.getUserId());
        body.put("name", shelf.getName().trim());
        if (shelf.getSystemKey() != null && !shelf.getSystemKey().isBlank()) {
            body.put("system_key", shelf.getSystemKey().trim());
        }
        return supabase.post(TABLE, body, ReadingShelf.class);
    }
}
