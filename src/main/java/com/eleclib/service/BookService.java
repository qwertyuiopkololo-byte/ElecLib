package com.eleclib.service;

import com.eleclib.dto.BookCardDto;
import com.eleclib.dto.ReviewDto;
import com.eleclib.model.Author;
import com.eleclib.model.Book;
import com.eleclib.model.BookRating;
import com.eleclib.model.User;
import com.eleclib.repository.AuthorRepository;
import com.eleclib.repository.BookRatingRepository;
import com.eleclib.repository.BookRepository;
import com.eleclib.repository.FavoriteRepository;
import com.eleclib.repository.GenreRepository;
import com.eleclib.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final BookRatingRepository bookRatingRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public List<BookCardDto> searchByTitleOrAuthor(String query, User currentUser) {
        if (query == null || query.isBlank()) {
            return findAllAsCards(currentUser);
        }
        Map<Long, String> authorNames = authorRepository.findAll().stream()
                .collect(Collectors.toMap(Author::getAuthorId, Author::getFullName));
        List<Book> books = bookRepository.searchByTitleOrAuthor(query.trim(), authorNames);
        return toCardDtos(books, currentUser);
    }

    public List<BookCardDto> findByGenre(Long genreId, User currentUser) {
        List<Book> books = bookRepository.findByGenre_GenreId(genreId);
        return toCardDtos(books, currentUser);
    }

    public List<BookCardDto> findAllAsCards(User currentUser) {
        return toCardDtos(bookRepository.findAll(), currentUser);
    }

    public Book findById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    public BookCardDto findBookCardById(Long bookId, User currentUser) {
        Book book = findById(bookId);
        if (book == null) return null;
        List<BookCardDto> one = toCardDtos(List.of(book), currentUser);
        return one.isEmpty() ? null : one.get(0);
    }

    /**
     * Карточки в порядке {@code bookIds} (для полок).
     */
    public List<BookCardDto> findCardsByBookIdsInOrder(List<Long> bookIds, User currentUser) {
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }
        List<Book> ordered = new ArrayList<>();
        for (Long id : bookIds) {
            bookRepository.findById(id).ifPresent(ordered::add);
        }
        return toCardDtos(ordered, currentUser);
    }

    public double getAverageRating(Long bookId) {
        Double avg = bookRatingRepository.getAverageRatingByBookId(bookId);
        return avg == null ? 0 : avg;
    }

    public List<ReviewDto> getReviews(Long bookId, Long currentUserId) {
        List<BookRating> ratings = bookRatingRepository.findByBook_BookId(bookId);
        List<Long> userIds = ratings.stream().map(BookRating::getUserId).distinct().toList();
        Map<Long, User> userMap = userIds.stream()
                .map(userRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toMap(User::getUserId, u -> u));
        return ratings.stream()
                .map(br -> {
                    User u = userMap.get(br.getUserId());
                    return ReviewDto.builder()
                            .userFullName(u != null ? u.getFullName() : "?")
                            .rating(br.getRating())
                            .review(br.getReview())
                            .createdAt(br.getCreatedAt())
                            .isCurrentUser(currentUserId != null && currentUserId.equals(br.getUserId()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    public boolean hasSubscriptionAccess(User user) {
        return subscriptionService.hasActiveSubscription(user);
    }

    public List<BookCardDto> getRecommendedBooks(User user, int limit) {
        if (user == null || limit <= 0) return List.of();
        List<com.eleclib.model.Favorite> favs = favoriteRepository.findByUser_UserId(user.getUserId());
        if (favs.isEmpty()) return List.of();
        Set<Long> favBookIds = favs.stream().map(com.eleclib.model.Favorite::getBookId).collect(Collectors.toSet());

        List<Book> favBooks = favBookIds.stream()
                .map(id -> bookRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        Set<Long> genreIds = favBooks.stream().map(Book::getGenreId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> authorIds = favBooks.stream().map(Book::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (genreIds.isEmpty() && authorIds.isEmpty()) return List.of();

        List<Book> all = bookRepository.findAll();
        List<Book> recommended = all.stream()
                .filter(b -> !favBookIds.contains(b.getBookId()))
                .filter(b -> (b.getGenreId() != null && genreIds.contains(b.getGenreId()))
                        || (b.getAuthorId() != null && authorIds.contains(b.getAuthorId())))
                .collect(Collectors.toList());

        Collections.shuffle(recommended);
        recommended = recommended.stream().limit(limit).toList();
        return toCardDtos(recommended, user);
    }

    private List<BookCardDto> toCardDtos(List<Book> books, User currentUser) {
        Long userId = currentUser != null ? currentUser.getUserId() : null;
        boolean hasSubscription = currentUser != null && hasSubscriptionAccess(currentUser);
        Map<Long, Author> authorMap = authorRepository.findAll().stream().collect(Collectors.toMap(Author::getAuthorId, a -> a));
        Map<Long, String> genreMap = genreRepository.findAll().stream().collect(Collectors.toMap(g -> g.getGenreId(), g -> g.getName()));
        return books.stream().map(b -> {
            double avg = getAverageRating(b.getBookId());
            int count = bookRatingRepository.findByBook_BookId(b.getBookId()).size();
            boolean inFav = userId != null && favoriteRepository.existsByUser_UserIdAndBook_BookId(userId, b.getBookId());
            Author author = b.getAuthorId() != null ? authorMap.get(b.getAuthorId()) : null;
            String genreName = b.getGenreId() != null ? genreMap.get(b.getGenreId()) : null;
            return BookCardDto.builder()
                    .bookId(b.getBookId())
                    .title(b.getTitle())
                    .authorName(author != null ? author.getFullName() : "")
                    .authorId(b.getAuthorId())
                    .genreName(genreName != null ? genreName : "")
                    .genreId(b.getGenreId())
                    .description(b.getDescription())
                    .averageRating(avg)
                    .reviewCount(count)
                    .inFavorites(inFav)
                    .hasProgress(false)
                    .lastPosition(0)
                    .subscriptionRequired(!hasSubscription)
                    .build();
        }).collect(Collectors.toList());
    }
}
