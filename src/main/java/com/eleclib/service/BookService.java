package com.eleclib.service;

import com.eleclib.dto.BookCardDto;
import com.eleclib.dto.ReviewDto;
import com.eleclib.model.Author;
import com.eleclib.model.Book;
import com.eleclib.model.BookRating;
import com.eleclib.model.User;
import com.eleclib.model.ReadingPosition;
import com.eleclib.model.ReadingShelf;
import com.eleclib.model.ReadingShelfBook;
import com.eleclib.repository.AuthorRepository;
import com.eleclib.repository.BookRatingRepository;
import com.eleclib.repository.BookRepository;
import com.eleclib.repository.FavoriteRepository;
import com.eleclib.repository.GenreRepository;
import com.eleclib.repository.ReadingPositionRepository;
import com.eleclib.repository.ReadingShelfBookRepository;
import com.eleclib.repository.ReadingShelfRepository;
import com.eleclib.repository.UserRepository;
import com.eleclib.util.ReaderPaging;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final ReadingPositionRepository readingPositionRepository;
    private final ReadingShelfRepository readingShelfRepository;
    private final ReadingShelfBookRepository readingShelfBookRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final ComicPdfStorageService comicPdfStorageService;

    private static final double WEIGHT_FAVORITE = 1.0;
    private static final double WEIGHT_SHELF_WANT = 0.5;
    private static final double WEIGHT_SHELF_DONE = 1.2;
    private static final double WEIGHT_SHELF_READING = 2.5;
    private static final double WEIGHT_SHELF_CUSTOM = 0.7;
    private static final double WEIGHT_FINISHED_READ = 1.0;
    private static final double WEIGHT_IN_PROGRESS = 2.0;
    private static final double RECENT_GENRE_AUTHOR_BOOST = 4.0;

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

    /** Все комиксы в каталоге (content_type = comic). */
    public List<BookCardDto> findComicsAsCards(User currentUser) {
        return toCardDtos(
                bookRepository.findAll().stream().filter(Book::isComic).toList(),
                currentUser);
    }

    /** Рекомендации среди комиксов для страницы /comics. */
    public List<BookCardDto> getRecommendedComics(User user, int limit) {
        if (user == null || limit <= 0) {
            return List.of();
        }
        Set<Long> comicIds = bookRepository.findAll().stream()
                .filter(Book::isComic)
                .map(Book::getBookId)
                .collect(Collectors.toSet());
        return getRecommendedBooks(user, limit * 3).stream()
                .filter(c -> comicIds.contains(c.getBookId()))
                .limit(limit)
                .toList();
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
        if (user == null || limit <= 0) {
            return List.of();
        }
        Long userId = user.getUserId();
        Set<Long> excludeBookIds = new HashSet<>();
        favoriteRepository.findByUser_UserId(userId).stream()
                .map(com.eleclib.model.Favorite::getBookId)
                .forEach(excludeBookIds::add);

        Map<Long, Double> genreWeights = new HashMap<>();
        Map<Long, Double> authorWeights = new HashMap<>();
        List<Book> recentActivityBooks = new ArrayList<>();

        for (com.eleclib.model.Favorite fav : favoriteRepository.findByUser_UserId(userId)) {
            bookRepository.findById(fav.getBookId()).ifPresent(book -> {
                excludeBookIds.add(book.getBookId());
                applySeedWeights(book, WEIGHT_FAVORITE, genreWeights, authorWeights);
            });
        }

        List<ReadingPosition> positions = readingPositionRepository.findByUserId(userId);
        List<ReadingPosition> byRecentUpdate = positions.stream()
                .sorted(Comparator.comparing(ReadingPosition::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        Set<Long> recentBookIdsSeen = new HashSet<>();
        for (ReadingPosition pos : byRecentUpdate) {
            bookRepository.findById(pos.getBookId()).ifPresent(book -> {
                excludeBookIds.add(book.getBookId());
                int total = ReaderPaging.totalPages(book.getText());
                int last = pos.getLastPage() != null ? pos.getLastPage() : 1;
                boolean inProgress = last < total;
                double base = inProgress ? WEIGHT_IN_PROGRESS : WEIGHT_FINISHED_READ;
                double weight = base * recencyFactor(pos.getUpdatedAt());
                applySeedWeights(book, weight, genreWeights, authorWeights);
                if (inProgress && recentBookIdsSeen.add(book.getBookId())) {
                    recentActivityBooks.add(book);
                }
            });
        }

        for (ReadingShelf shelf : readingShelfRepository.findByUserId(userId)) {
            double shelfBase = shelfWeightForKey(shelf.getSystemKey());
            for (ReadingShelfBook sb : readingShelfBookRepository.findByShelfId(shelf.getShelfId())) {
                bookRepository.findById(sb.getBookId()).ifPresent(book -> {
                    excludeBookIds.add(book.getBookId());
                    double weight = shelfBase * recencyFactor(sb.getAddedAt());
                    applySeedWeights(book, weight, genreWeights, authorWeights);
                    if (ReadingShelfService.SK_READING.equals(shelf.getSystemKey())
                            && recentBookIdsSeen.add(book.getBookId())) {
                        recentActivityBooks.add(book);
                    }
                });
            }
        }

        if (genreWeights.isEmpty() && authorWeights.isEmpty()) {
            return List.of();
        }

        Set<Long> recentGenreIds = new LinkedHashSet<>();
        Set<Long> recentAuthorIds = new LinkedHashSet<>();
        int boostBooks = 0;
        for (Book book : recentActivityBooks) {
            if (boostBooks >= 3) {
                break;
            }
            boostBooks++;
            if (book.getGenreId() != null) {
                recentGenreIds.add(book.getGenreId());
            }
            if (book.getAuthorId() != null) {
                recentAuthorIds.add(book.getAuthorId());
            }
        }
        if (recentGenreIds.isEmpty() && recentAuthorIds.isEmpty()) {
            for (ReadingPosition pos : byRecentUpdate) {
                if (boostBooks >= 3) {
                    break;
                }
                Optional<Book> bookOpt = bookRepository.findById(pos.getBookId());
                if (bookOpt.isEmpty()) {
                    continue;
                }
                Book book = bookOpt.get();
                boostBooks++;
                if (book.getGenreId() != null) {
                    recentGenreIds.add(book.getGenreId());
                }
                if (book.getAuthorId() != null) {
                    recentAuthorIds.add(book.getAuthorId());
                }
            }
        }

        final Set<Long> boostGenres = recentGenreIds;
        final Set<Long> boostAuthors = recentAuthorIds;

        List<Book> recommended = bookRepository.findAll().stream()
                .filter(b -> !excludeBookIds.contains(b.getBookId()))
                .filter(b -> scoreCandidate(b, genreWeights, authorWeights, boostGenres, boostAuthors) > 0)
                .sorted((a, b) -> Double.compare(
                        scoreCandidate(b, genreWeights, authorWeights, boostGenres, boostAuthors),
                        scoreCandidate(a, genreWeights, authorWeights, boostGenres, boostAuthors)))
                .limit(limit)
                .toList();
        return toCardDtos(recommended, user);
    }

    private static double shelfWeightForKey(String systemKey) {
        if (systemKey == null || systemKey.isBlank()) {
            return WEIGHT_SHELF_CUSTOM;
        }
        return switch (systemKey) {
            case ReadingShelfService.SK_READING -> WEIGHT_SHELF_READING;
            case ReadingShelfService.SK_DONE -> WEIGHT_SHELF_DONE;
            case ReadingShelfService.SK_WANT -> WEIGHT_SHELF_WANT;
            default -> WEIGHT_SHELF_CUSTOM;
        };
    }

    private static double recencyFactor(Instant instant) {
        if (instant == null) {
            return 0.6;
        }
        long days = ChronoUnit.DAYS.between(instant, Instant.now());
        return Math.max(0.35, 1.6 - days / 12.0);
    }

    private static void applySeedWeights(Book book, double weight,
                                         Map<Long, Double> genreWeights, Map<Long, Double> authorWeights) {
        if (weight <= 0) {
            return;
        }
        if (book.getGenreId() != null) {
            genreWeights.merge(book.getGenreId(), weight, Double::sum);
        }
        if (book.getAuthorId() != null) {
            authorWeights.merge(book.getAuthorId(), weight, Double::sum);
        }
    }

    private static double scoreCandidate(Book book, Map<Long, Double> genreWeights, Map<Long, Double> authorWeights,
                                         Set<Long> recentGenres, Set<Long> recentAuthors) {
        double score = 0;
        if (book.getGenreId() != null) {
            score += genreWeights.getOrDefault(book.getGenreId(), 0.0);
        }
        if (book.getAuthorId() != null) {
            score += authorWeights.getOrDefault(book.getAuthorId(), 0.0);
        }
        if (book.getGenreId() != null && recentGenres.contains(book.getGenreId())) {
            score += RECENT_GENRE_AUTHOR_BOOST;
        }
        if (book.getAuthorId() != null && recentAuthors.contains(book.getAuthorId())) {
            score += RECENT_GENRE_AUTHOR_BOOST;
        }
        return score;
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
                    .coverUrl(b.getCoverUrl())
                    .averageRating(avg)
                    .reviewCount(count)
                    .inFavorites(inFav)
                    .hasProgress(false)
                    .lastPosition(0)
                    .subscriptionRequired(!hasSubscription)
                    .comic(b.isComic())
                    .comicReady(b.isComic() && comicPdfStorageService.hasPdf(b.getBookId()))
                    .build();
        }).collect(Collectors.toList());
    }
}
