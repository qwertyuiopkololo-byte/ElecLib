package com.eleclib.repository;

import com.eleclib.model.Genre;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GenreRepository {

    private static final long CACHE_TTL_NANOS = java.time.Duration.ofMinutes(1).toNanos();

    private static final String TABLE = "genres";
    private static final String PK = "genre_id";

    private final SupabaseClient supabase;
    private volatile List<Genre> cachedGenres;
    private volatile long genresCachedAt;

    public List<Genre> findAll() {
        List<Genre> cached = cachedGenres;
        long now = System.nanoTime();
        if (cached != null && now - genresCachedAt < CACHE_TTL_NANOS) return cached;
        synchronized (this) {
            cached = cachedGenres;
            now = System.nanoTime();
            if (cached == null || now - genresCachedAt >= CACHE_TTL_NANOS) {
                cached = List.copyOf(supabase.getAll(TABLE, Genre.class));
                cachedGenres = cached;
                genresCachedAt = now;
            }
            return cached;
        }
    }

    public Optional<Genre> findById(Long id) {
        return Optional.ofNullable(supabase.getById(TABLE, PK, id, Genre.class));
    }

    public Genre save(Genre genre) {
        Genre saved;
        if (genre.getGenreId() == null || genre.getGenreId() <= 0) {
            genre.setGenreId(null);
            Genre created = supabase.post(TABLE, PK, genre, Genre.class);
            saved = created != null ? created : genre;
        } else {
            supabase.patch(TABLE, PK, genre.getGenreId(), genre);
            saved = genre;
        }
        cachedGenres = null;
        return saved;
    }

    public long count() {
        return findAll().size();
    }
}
