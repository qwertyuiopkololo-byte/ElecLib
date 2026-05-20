package com.eleclib.service;

import com.eleclib.dto.BookCardDto;
import com.eleclib.dto.ReadingShelfSummaryDto;
import com.eleclib.model.ReadingShelf;
import com.eleclib.model.ReadingShelfBook;
import com.eleclib.model.User;
import com.eleclib.repository.ReadingShelfBookRepository;
import com.eleclib.repository.ReadingShelfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingShelfService {

    public static final String SK_WANT = "want";
    public static final String SK_READING = "reading";
    public static final String SK_DONE = "done";

    private static final Map<String, String> DEFAULT_NAMES = Map.of(
            SK_WANT, "Хочу прочитать",
            SK_READING, "Читаю",
            SK_DONE, "Прочитано"
    );

    private static final List<String> SYSTEM_ORDER = List.of(SK_WANT, SK_READING, SK_DONE);

    private final ReadingShelfRepository shelfRepository;
    private final ReadingShelfBookRepository shelfBookRepository;
    private final BookService bookService;

    public void ensureDefaultShelves(Long userId) {
        List<ReadingShelf> existing = shelfRepository.findByUserId(userId);
        Set<String> present = existing.stream()
                .map(ReadingShelf::getSystemKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (String key : SYSTEM_ORDER) {
            if (!present.contains(key)) {
                shelfRepository.save(ReadingShelf.builder()
                        .userId(userId)
                        .name(DEFAULT_NAMES.get(key))
                        .systemKey(key)
                        .build());
            }
        }
    }

    public List<ReadingShelf> listShelvesSorted(Long userId) {
        ensureDefaultShelves(userId);
        List<ReadingShelf> shelves = shelfRepository.findByUserId(userId);
        Comparator<ReadingShelf> cmp = Comparator
                .comparing((ReadingShelf s) -> s.getSystemKey() == null ? 1 : 0)
                .thenComparing(s -> {
                    String k = s.getSystemKey();
                    if (k == null) {
                        return 99;
                    }
                    int idx = SYSTEM_ORDER.indexOf(k);
                    return idx < 0 ? 50 : idx;
                })
                .thenComparing(ReadingShelf::getShelfId, Comparator.nullsLast(Long::compareTo));
        return shelves.stream().sorted(cmp).toList();
    }

    public List<ReadingShelfSummaryDto> listSummaries(Long userId) {
        return listShelvesSorted(userId).stream()
                .map(s -> ReadingShelfSummaryDto.builder()
                        .shelf(s)
                        .bookCount(shelfBookRepository.findByShelfId(s.getShelfId()).size())
                        .build())
                .toList();
    }

    public List<BookCardDto> listBooksOnShelf(Long userId, Long shelfId, User currentUser) {
        ReadingShelf shelf = shelfRepository.findById(shelfId);
        if (shelf == null || !userId.equals(shelf.getUserId())) {
            return List.of();
        }
        List<Long> bookIds = shelfBookRepository.findByShelfId(shelfId).stream()
                .map(ReadingShelfBook::getBookId)
                .filter(Objects::nonNull)
                .toList();
        return bookService.findCardsByBookIdsInOrder(bookIds, currentUser);
    }

    public Optional<ReadingShelf> findShelfIfOwned(Long shelfId, Long userId) {
        ReadingShelf s = shelfRepository.findById(shelfId);
        if (s == null || !userId.equals(s.getUserId())) {
            return Optional.empty();
        }
        return Optional.of(s);
    }

    /**
     * ID полок пользователя, на которых есть эта книга.
     */
    public Set<Long> shelfIdsContainingBook(Long userId, Long bookId) {
        ensureDefaultShelves(userId);
        Set<Long> userShelfIds = shelfRepository.findByUserId(userId).stream()
                .map(ReadingShelf::getShelfId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return shelfBookRepository.findByBookId(bookId).stream()
                .map(ReadingShelfBook::getShelfId)
                .filter(userShelfIds::contains)
                .collect(Collectors.toSet());
    }

    public Optional<String> findSystemShelfKeyForBook(Long userId, Long bookId) {
        ensureDefaultShelves(userId);
        Map<Long, ReadingShelf> byId = shelfRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(ReadingShelf::getShelfId, s -> s, (a, b) -> a));
        return shelfBookRepository.findByBookId(bookId).stream()
                .map(ReadingShelfBook::getShelfId)
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(ReadingShelf::getSystemKey)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public void addBookToShelf(Long userId, Long shelfId, Long bookId) {
        ReadingShelf shelf = shelfRepository.findById(shelfId);
        if (shelf == null || !userId.equals(shelf.getUserId())) {
            throw new IllegalArgumentException("Полка не найдена");
        }
        if (shelf.getSystemKey() != null && !shelf.getSystemKey().isBlank()) {
            List<ReadingShelf> all = shelfRepository.findByUserId(userId);
            for (ReadingShelf other : all) {
                if (other.getSystemKey() != null
                        && !other.getSystemKey().equals(shelf.getSystemKey())
                        && other.getShelfId() != null) {
                    shelfBookRepository.delete(other.getShelfId(), bookId);
                }
            }
        }
        boolean already = shelfBookRepository.findByShelfId(shelfId).stream()
                .anyMatch(sb -> bookId.equals(sb.getBookId()));
        if (!already) {
            shelfBookRepository.save(shelfId, bookId);
        }
    }

    public void removeBookFromShelf(Long userId, Long shelfId, Long bookId) {
        ReadingShelf shelf = shelfRepository.findById(shelfId);
        if (shelf == null || !userId.equals(shelf.getUserId())) {
            throw new IllegalArgumentException("Полка не найдена");
        }
        shelfBookRepository.delete(shelfId, bookId);
    }

    public void createCustomShelf(Long userId, String name) {
        String n = name != null ? name.trim() : "";
        if (n.isEmpty() || n.length() > 120) {
            throw new IllegalArgumentException("Укажите название полки (до 120 символов)");
        }
        shelfRepository.save(ReadingShelf.builder()
                .userId(userId)
                .name(n)
                .systemKey(null)
                .build());
    }
}
