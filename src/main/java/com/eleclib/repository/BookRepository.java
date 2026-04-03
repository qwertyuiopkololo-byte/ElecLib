package com.eleclib.repository;

import com.eleclib.model.Book;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class BookRepository {

    private static final String TABLE = "books";
    private static final String PK = "book_id";

    private final SupabaseClient supabase;

    public List<Book> findAll() {
        return supabase.getAll(TABLE, Book.class);
    }

    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(supabase.getById(TABLE, PK, id, Book.class));
    }

    public List<Book> findByGenre_GenreId(Long genreId) {
        return supabase.getByFilter(TABLE, "genre_id", genreId, Book.class);
    }

    public List<Book> searchByTitleOrAuthor(String query, java.util.Map<Long, String> authorFullNames) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) return findAll();
        return findAll().stream()
                .filter(b -> {
                    boolean titleMatch = b.getTitle() != null && b.getTitle().toLowerCase().contains(q);
                    String authorName = authorFullNames != null && b.getAuthorId() != null ? authorFullNames.get(b.getAuthorId()) : null;
                    boolean authorMatch = authorName != null && authorName.toLowerCase().contains(q);
                    return titleMatch || authorMatch;
                })
                .collect(Collectors.toList());
    }

    public Book save(Book book) {
        if (book.getBookId() == null) {
            Book created = supabase.post(TABLE, book, Book.class);
            return created != null ? created : book;
        }
        supabase.patch(TABLE, PK, book.getBookId(), book);
        return book;
    }

    public long count() {
        return findAll().size();
    }
}
