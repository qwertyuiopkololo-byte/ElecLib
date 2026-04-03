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

    private static final String TABLE = "authors";
    private static final String PK = "author_id";

    private final SupabaseClient supabase;

    public List<Author> findAll() {
        return supabase.getAll(TABLE, Author.class);
    }

    public Optional<Author> findById(Long id) {
        return Optional.ofNullable(supabase.getById(TABLE, PK, id, Author.class));
    }

    public Author save(Author author) {
        if (author.getAuthorId() == null) {
            Author created = supabase.post(TABLE, author, Author.class);
            return created != null ? created : author;
        }
        supabase.patch(TABLE, PK, author.getAuthorId(), author);
        return author;
    }

    public long count() {
        return findAll().size();
    }
}
