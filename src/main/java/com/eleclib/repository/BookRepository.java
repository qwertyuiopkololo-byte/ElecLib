package com.eleclib.repository;

import com.eleclib.model.Book;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
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

    /** Loads a lightweight, stable slice of the catalog without book text. */
    public List<Book> findCatalogPage(int offset, int limit, Long genreId,
                                      String query, List<Long> authorIds) {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("select", "book_id,title,description,author_id,genre_id,cover_url,content_type");
        filters.put("order", "book_id.asc");
        filters.put("offset", String.valueOf(offset));
        filters.put("limit", String.valueOf(limit));
        if (genreId != null) {
            filters.put("genre_id", "eq." + genreId);
        }
        if (query != null && !query.isBlank()) {
            String safeQuery = query.trim().replaceAll("[(),*%]", " ").trim();
            if (!safeQuery.isEmpty()) {
                String titleFilter = "title.ilike.*" + safeQuery + "*";
                if (authorIds != null && !authorIds.isEmpty()) {
                    String ids = authorIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                    filters.put("or", "(" + titleFilter + ",author_id.in.(" + ids + "))");
                } else {
                    filters.put("title", "ilike.*" + safeQuery + "*");
                }
            }
        }
        return supabase.get(TABLE, filters, Book.class);
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
        Long id = book.getBookId();
        if (id == null || id <= 0) {
            book.setBookId(null);
            Book created = supabase.post(TABLE, PK, book, Book.class);
            return created != null ? created : book;
        }
        supabase.patch(TABLE, PK, id, book);
        return book;
    }

    public long count() {
        return findAll().size();
    }
}
