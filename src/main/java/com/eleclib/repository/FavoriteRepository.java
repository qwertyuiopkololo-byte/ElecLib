package com.eleclib.repository;

import com.eleclib.model.Favorite;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class FavoriteRepository {

    private static final String TABLE = "favorites";

    private final SupabaseClient supabase;

    public List<Favorite> findByUser_UserId(Long userId) {
        return supabase.getByFilter(TABLE, "user_id", userId, Favorite.class);
    }

    public boolean existsByUser_UserIdAndBook_BookId(Long userId, Long bookId) {
        List<Favorite> list = supabase.get(TABLE, Map.of("user_id", "eq." + userId, "book_id", "eq." + bookId), Favorite.class);
        return !list.isEmpty();
    }

    public void deleteByUser_UserIdAndBook_BookId(Long userId, Long bookId) {
        supabase.deleteComposite(TABLE, Map.of("user_id", userId, "book_id", bookId));
    }

    public Favorite save(Favorite f) {
        return supabase.post(TABLE, f, Favorite.class);
    }
}
