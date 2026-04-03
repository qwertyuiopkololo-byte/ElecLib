package com.eleclib.service;

import com.eleclib.dto.BookCardDto;
import com.eleclib.model.Author;
import com.eleclib.model.Book;
import com.eleclib.model.Favorite;
import com.eleclib.model.Genre;
import com.eleclib.model.User;
import com.eleclib.repository.AuthorRepository;
import com.eleclib.repository.BookRatingRepository;
import com.eleclib.repository.BookRepository;
import com.eleclib.repository.FavoriteRepository;
import com.eleclib.repository.GenreRepository;
import com.eleclib.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceRecommendationTest {

    @Mock BookRepository bookRepository;
    @Mock AuthorRepository authorRepository;
    @Mock GenreRepository genreRepository;
    @Mock BookRatingRepository bookRatingRepository;
    @Mock FavoriteRepository favoriteRepository;
    @Mock UserRepository userRepository;
    @Mock SubscriptionService subscriptionService;

    @InjectMocks
    BookService bookService;

    @Test
    void getRecommendedBooks_returnsBooksWithSameGenreOrAuthor_excludingFavorites() {
        User user = User.builder().userId(100L).firstName("Иван").lastName("Иванов").build();

        // favorites: book 1
        when(favoriteRepository.findByUser_UserId(100L))
                .thenReturn(List.of(Favorite.builder().userId(100L).bookId(1L).build()));

        Book favBook = Book.builder().bookId(1L).title("Fav").authorId(10L).genreId(20L).text("t").build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(favBook));

        Book sameGenre = Book.builder().bookId(2L).title("SameGenre").authorId(11L).genreId(20L).text("t").build();
        Book sameAuthor = Book.builder().bookId(3L).title("SameAuthor").authorId(10L).genreId(21L).text("t").build();
        Book unrelated = Book.builder().bookId(4L).title("Other").authorId(99L).genreId(99L).text("t").build();
        when(bookRepository.findAll()).thenReturn(List.of(favBook, sameGenre, sameAuthor, unrelated));

        when(authorRepository.findAll()).thenReturn(List.of(
                Author.builder().authorId(10L).firstName("A").lastName("Fav").build(),
                Author.builder().authorId(11L).firstName("B").lastName("Genre").build(),
                Author.builder().authorId(99L).firstName("C").lastName("Other").build()
        ));
        when(genreRepository.findAll()).thenReturn(List.of(
                Genre.builder().genreId(20L).name("G20").build(),
                Genre.builder().genreId(21L).name("G21").build(),
                Genre.builder().genreId(99L).name("G99").build()
        ));

        when(subscriptionService.hasActiveSubscription(user)).thenReturn(true);
        when(bookRatingRepository.getAverageRatingByBookId(anyLong())).thenReturn(null);
        when(bookRatingRepository.findByBook_BookId(anyLong())).thenReturn(List.of());
        when(favoriteRepository.existsByUser_UserIdAndBook_BookId(anyLong(), anyLong())).thenReturn(false);

        List<BookCardDto> recommended = bookService.getRecommendedBooks(user, 12);

        Set<Long> ids = recommended.stream().map(BookCardDto::getBookId).collect(Collectors.toSet());
        assertFalse(ids.contains(1L), "Избранная книга не должна рекомендоваться");
        assertTrue(ids.contains(2L), "Книга того же жанра должна рекомендоваться");
        assertTrue(ids.contains(3L), "Книга того же автора должна рекомендоваться");
        assertFalse(ids.contains(4L), "Несвязанная книга не должна рекомендоваться");
        assertTrue(recommended.size() <= 12);
    }

    @Test
    void getRecommendedBooks_returnsEmpty_whenNoFavorites() {
        User user = User.builder().userId(100L).build();
        when(favoriteRepository.findByUser_UserId(100L)).thenReturn(List.of());

        assertEquals(List.of(), bookService.getRecommendedBooks(user, 10));
        verify(bookRepository, never()).findAll();
    }
}

