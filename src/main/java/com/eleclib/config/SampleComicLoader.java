package com.eleclib.config;

import com.eleclib.model.Author;
import com.eleclib.model.Book;
import com.eleclib.model.Genre;
import com.eleclib.repository.AuthorRepository;
import com.eleclib.repository.BookRepository;
import com.eleclib.repository.GenreRepository;
import com.eleclib.service.ComicPdfStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

/**
 * При старте добавляет демо-комикс «Приключение в ElecLib», если его ещё нет в каталоге.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SampleComicLoader implements ApplicationRunner {

    private static final String DEMO_TITLE = "Приключение в ElecLib";
    private static final String DEMO_GENRE = "Комиксы";
    private static final String DEMO_AUTHOR_LAST = "Демо";
    private static final String DEMO_AUTHOR_FIRST = "Автор";

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final ComicPdfStorageService comicPdfStorageService;

    @Value("${eleclib.sample-comic.enabled:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            ensureDemoComic();
        } catch (Exception e) {
            log.warn("Demo comic not loaded: {}", e.getMessage());
        }
    }

    private void ensureDemoComic() throws Exception {
        Optional<Book> existing = bookRepository.findAll().stream()
                .filter(b -> DEMO_TITLE.equals(b.getTitle()))
                .findFirst();
        if (existing.isPresent()) {
            Book book = existing.get();
            if (book.isComic() && !comicPdfStorageService.hasPdf(book.getBookId())) {
                copySamplePdf(book.getBookId());
                log.info("Demo comic PDF restored for book id={}", book.getBookId());
            }
            return;
        }

        Author author = findOrCreateAuthor();
        Genre genre = findOrCreateGenre();
        Book book = Book.builder()
                .title(DEMO_TITLE)
                .description("Короткий демо-комикс на 4 страницы для проверки читалки PDF.")
                .text(".")
                .contentType(Book.CONTENT_TYPE_COMIC)
                .authorId(author.getAuthorId())
                .genreId(genre.getGenreId())
                .build();
        book = bookRepository.save(book);
        if (book.getBookId() == null) {
            log.warn("Could not create demo comic book");
            return;
        }
        copySamplePdf(book.getBookId());
        log.info("Demo comic created: id={}, title={}", book.getBookId(), DEMO_TITLE);
    }

    private void copySamplePdf(Long bookId) throws Exception {
        ClassPathResource resource = new ClassPathResource("samples/demo-comic.pdf");
        if (!resource.exists()) {
            log.warn("samples/demo-comic.pdf not found — run: node scripts/generate-sample-comic.mjs");
            return;
        }
        Path target = comicPdfStorageService.pathFor(bookId);
        Files.createDirectories(target.getParent());
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Author findOrCreateAuthor() {
        List<Author> all = authorRepository.findAll();
        for (Author a : all) {
            if (DEMO_AUTHOR_LAST.equals(a.getLastName()) && DEMO_AUTHOR_FIRST.equals(a.getFirstName())) {
                return a;
            }
        }
        Author created = authorRepository.save(Author.builder()
                .firstName(DEMO_AUTHOR_FIRST)
                .lastName(DEMO_AUTHOR_LAST)
                .biography("Демонстрационный автор для тестового комикса.")
                .build());
        return created != null ? created : Author.builder().firstName(DEMO_AUTHOR_FIRST).lastName(DEMO_AUTHOR_LAST).build();
    }

    private Genre findOrCreateGenre() {
        List<Genre> all = genreRepository.findAll();
        for (Genre g : all) {
            if (DEMO_GENRE.equalsIgnoreCase(g.getName())) {
                return g;
            }
        }
        Genre created = genreRepository.save(Genre.builder().name(DEMO_GENRE).build());
        return created != null ? created : Genre.builder().name(DEMO_GENRE).build();
    }
}
