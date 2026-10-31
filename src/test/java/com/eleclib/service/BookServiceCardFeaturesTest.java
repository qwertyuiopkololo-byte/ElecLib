package com.eleclib.service;

import com.eleclib.dto.BookCardDto;
import com.eleclib.model.Author;
import com.eleclib.model.Book;
import com.eleclib.model.Genre;
import com.eleclib.model.User;
import com.eleclib.repository.AuthorRepository;
import com.eleclib.repository.BookRatingRepository;
import com.eleclib.repository.BookRepository;
import com.eleclib.repository.FavoriteRepository;
import com.eleclib.repository.GenreRepository;
import com.eleclib.repository.ReadingPositionRepository;
import com.eleclib.repository.ReadingShelfBookRepository;
import com.eleclib.repository.ReadingShelfRepository;
import com.eleclib.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceCardFeaturesTest {

    @Mock BookRepository bookRepository;
    @Mock AuthorRepository authorRepository;
    @Mock GenreRepository genreRepository;
    @Mock BookRatingRepository bookRatingRepository;
    @Mock FavoriteRepository favoriteRepository;
    @Mock ReadingPositionRepository readingPositionRepository;
    @Mock ReadingShelfRepository readingShelfRepository;
    @Mock ReadingShelfBookRepository readingShelfBookRepository;
    @Mock UserRepository userRepository;
    @Mock SubscriptionService subscriptionService;
    @Mock ComicPdfStorageService comicPdfStorageService;

    @InjectMocks
    BookService bookService;

    private User subscriber;

    @BeforeEach
    void stubLookups() {
        subscriber = User.builder().userId(1L).login("reader").build();
        when(subscriptionService.hasActiveSubscription(subscriber)).thenReturn(true);
        when(authorRepository.findAll()).thenReturn(List.of(
                Author.builder().authorId(10L).firstName("Иван").lastName("Автор").build()
        ));
        when(genreRepository.findAll()).thenReturn(List.of(
                Genre.builder().genreId(20L).name("Фантастика").build()
        ));
        when(bookRatingRepository.getAverageRatingByBookId(anyLong())).thenReturn(null);
        when(bookRatingRepository.findByBook_BookId(anyLong())).thenReturn(List.of());
        when(favoriteRepository.existsByUser_UserIdAndBook_BookId(anyLong(), anyLong())).thenReturn(false);
    }

    @Test
    void findBookCardById_mapsCoverUrl() {
        Book book = Book.builder()
                .bookId(5L)
                .title("С обложкой")
                .authorId(10L)
                .genreId(20L)
                .coverUrl("https://cdn.example/cover.png")
                .contentType(Book.CONTENT_TYPE_TEXT)
                .build();
        when(bookRepository.findById(5L)).thenReturn(Optional.of(book));

        BookCardDto card = bookService.findBookCardById(5L, subscriber);

        assertEquals("https://cdn.example/cover.png", card.getCoverUrl());
        assertFalse(card.isComic());
    }

    @Test
    void findBookCardById_comicReadyWhenPdfOnDisk() {
        Book comic = Book.builder()
                .bookId(8L)
                .title("Комикс")
                .authorId(10L)
                .genreId(20L)
                .contentType(Book.CONTENT_TYPE_COMIC)
                .build();
        when(bookRepository.findById(8L)).thenReturn(Optional.of(comic));
        when(comicPdfStorageService.hasPdf(8L)).thenReturn(true);

        BookCardDto card = bookService.findBookCardById(8L, subscriber);

        assertTrue(card.isComic());
        assertTrue(card.isComicReady());
    }

    @Test
    void findBookCardById_comicNotReadyWithoutPdf() {
        Book comic = Book.builder()
                .bookId(9L)
                .title("Без PDF")
                .authorId(10L)
                .genreId(20L)
                .contentType(Book.CONTENT_TYPE_COMIC)
                .build();
        when(bookRepository.findById(9L)).thenReturn(Optional.of(comic));
        when(comicPdfStorageService.hasPdf(9L)).thenReturn(false);

        BookCardDto card = bookService.findBookCardById(9L, subscriber);

        assertTrue(card.isComic());
        assertFalse(card.isComicReady());
    }

    @Test
    void findComicsAsCards_filtersComicContentType() {
        Book text = Book.builder().bookId(1L).title("Текст").contentType(Book.CONTENT_TYPE_TEXT).authorId(10L).genreId(20L).build();
        Book comic = Book.builder().bookId(2L).title("Комикс").contentType(Book.CONTENT_TYPE_COMIC).authorId(10L).genreId(20L).build();
        when(bookRepository.findAll()).thenReturn(List.of(text, comic));
        when(comicPdfStorageService.hasPdf(2L)).thenReturn(true);

        var cards = bookService.findComicsAsCards(subscriber);

        assertEquals(1, cards.size());
        assertEquals(2L, cards.get(0).getBookId());
        assertTrue(cards.get(0).isComic());
    }

    @Test
    void getCatalogPageRequestsOneExtraAndLoadsRatingsInBatch() {
        List<Book> fetched = java.util.stream.LongStream.rangeClosed(1, 25)
                .mapToObj(id -> Book.builder().bookId(id).title("Book " + id)
                        .authorId(10L).genreId(20L).build())
                .toList();
        when(bookRepository.findCatalogPage(24, 25, 20L, "Автор", List.of(10L))).thenReturn(fetched);

        var page = bookService.getCatalogPage(1, "Автор", 20L, subscriber);

        assertEquals(24, page.books().size());
        assertEquals(1, page.page());
        assertTrue(page.hasNext());
        verify(bookRepository).findCatalogPage(24, 25, 20L, "Автор", List.of(10L));
        verify(bookRatingRepository).findByBookIds(anyList());
        verify(bookRatingRepository, never()).findByBook_BookId(anyLong());
        verify(favoriteRepository).findByUserAndBookIds(eq(1L), anyList());
        verify(favoriteRepository, never()).existsByUser_UserIdAndBook_BookId(anyLong(), anyLong());
    }
}
