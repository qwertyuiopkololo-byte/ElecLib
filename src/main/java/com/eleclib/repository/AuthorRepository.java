package com.eleclib.repository;

import com.eleclib.model.Author;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthorRepository {

    private static final long CACHE_TTL_NANOS = java.time.Duration.ofMinutes(1).toNanos();

    private static final String TABLE = "authors";
    private static final String PK = "author_id";

    private final SupabaseClient supabase;
    private volatile List<Author> cachedAuthors;
    private volatile long authorsCachedAt;

    public List<Author> findAll() {
        List<Author> cached = cachedAuthors;
        long now = System.nanoTime();
        if (cached != null && now - authorsCachedAt < CACHE_TTL_NANOS) return cached;
        synchronized (this) {
            cached = cachedAuthors;
            now = System.nanoTime();
            if (cached == null || now - authorsCachedAt >= CACHE_TTL_NANOS) {
                cached = List.copyOf(supabase.getAll(TABLE, Author.class));
                cachedAuthors = cached;
                authorsCachedAt = now;
            }
            return cached;
        }
    }

    public Optional<Author> findById(Long id) {
        return Optional.ofNullable(supabase.getById(TABLE, PK, id, Author.class));
    }

    public Author save(Author author) {
        Author saved;
        if (author.getAuthorId() == null || author.getAuthorId() <= 0) {
            author.setAuthorId(null);
            Author created = supabase.post(TABLE, PK, author, Author.class);
            saved = created != null ? created : author;
        } else {
            supabase.patch(TABLE, PK, author.getAuthorId(), author);
            saved = author;
        }
        cachedAuthors = null;
        return saved;
    }

    public long count() {
        return findAll().size();
    }
}
