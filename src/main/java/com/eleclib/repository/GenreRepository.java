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

    private static final String TABLE = "genres";
    private static final String PK = "genre_id";

    private final SupabaseClient supabase;

    public List<Genre> findAll() {
        return supabase.getAll(TABLE, Genre.class);
    }

    public Optional<Genre> findById(Long id) {
        return Optional.ofNullable(supabase.getById(TABLE, PK, id, Genre.class));
    }

    public Genre save(Genre genre) {
        if (genre.getGenreId() == null) {
            Genre created = supabase.post(TABLE, genre, Genre.class);
            return created != null ? created : genre;
        }
        supabase.patch(TABLE, PK, genre.getGenreId(), genre);
        return genre;
    }

    public long count() {
        return findAll().size();
    }
}
