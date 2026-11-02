package com.eleclib.service;

import com.eleclib.dto.BookCardDto;
import com.eleclib.model.*;
import com.eleclib.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.LongStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookServiceNewFeaturesTest {
    BookRepository books;
    ReadingPositionRepository positions;
    FavoriteRepository favorites;
    ReadingShelfRepository shelves;
    ReadingShelfBookRepository shelfBooks;
    BookService service;
    User user = User.builder().userId(7L).build();
    @BeforeEach void setUp() {
        books = mock(BookRepository.class); positions = mock(ReadingPositionRepository.class);
        favorites = mock(FavoriteRepository.class); shelves = mock(ReadingShelfRepository.class);
        shelfBooks = mock(ReadingShelfBookRepository.class);
        service = new BookService(books, mock(AuthorRepository.class), mock(GenreRepository.class),
                mock(BookRatingRepository.class), favorites, positions, shelves, shelfBooks,
                mock(UserRepository.class), mock(SubscriptionService.class), mock(ComicPdfStorageService.class));
    }
    Book book(long id, long author) {
        return Book.builder().bookId(id).authorId(author).genreId(20L).title("Book " + id).text("x".repeat(5000)).build();
    }
    BookCardDto card(int page) {
        when(books.findById(12L)).thenReturn(Optional.of(book(12L, 10L)));
        when(positions.findByUserId(7L)).thenReturn(List.of(ReadingPosition.builder().bookId(12L).lastPage(page).build()));
        when(books.findTextsByBookIds(List.of(12L))).thenReturn(List.of(book(12L, 10L)));
        return service.findBookCardById(12L, user);
    }
    @Test void unreadCardHasZeroPercent() {
        when(books.findById(12L)).thenReturn(Optional.of(book(12L, 10L)));
        var card = service.findBookCardById(12L, user);
        assertFalse(card.isHasProgress()); assertEquals(0, card.getProgressPercent());
    }
    @Test void firstOfTwoPagesShowsFiftyPercent() {
        var card = card(1);
        assertTrue(card.isHasProgress()); assertEquals(50, card.getProgressPercent()); assertEquals(2, card.getTotalPages());
    }
    @Test void outOfRangePageIsClampedToBookLength() {
        var card = card(99);
        assertEquals(2, card.getLastPosition()); assertEquals(100, card.getProgressPercent());
    }
    @Test void invalidStoredPageIsClampedToFirstPage() {
        var card = card(-3);
        assertEquals(1, card.getLastPosition()); assertEquals(50, card.getProgressPercent());
    }
    @Test void similarBooksExcludeCurrentFavoritesAndCompletedAndPrioritizeAuthor() {
        Book current = book(1L, 10L);
        when(books.findSimilarBooks(10L, 20L, 1L, 64))
                .thenReturn(List.of(current, book(2L, 10L), book(3L, 10L), book(4L, 99L), book(5L, 10L)));
        when(favorites.findByUser_UserId(7L)).thenReturn(List.of(Favorite.builder().bookId(2L).build()));
        when(shelves.findByUserId(7L)).thenReturn(List.of(ReadingShelf.builder().shelfId(30L).systemKey("done").build()));
        when(shelfBooks.findByShelfId(30L)).thenReturn(List.of(ReadingShelfBook.builder().bookId(3L).build()));
        assertEquals(List.of(5L, 4L), service.getSimilarBooks(current, user, 8).stream().map(BookCardDto::getBookId).toList());
    }
    @Test void similarBooksRespectEightBookLimitForGuest() {
        when(books.findSimilarBooks(10L, 20L, 1L, 64))
                .thenReturn(LongStream.rangeClosed(2, 15).mapToObj(id -> book(id, 10L)).toList());
        assertEquals(8, service.getSimilarBooks(book(1L, 10L), null, 8).size());
        verifyNoInteractions(favorites, shelves);
    }
    @Test void missingBookOrZeroLimitDoesNotQueryStorage() {
        assertTrue(service.getSimilarBooks(null, user, 8).isEmpty());
        assertTrue(service.getSimilarBooks(book(1L, 10L), user, 0).isEmpty());
        verifyNoInteractions(books);
    }
}
