package com.eleclib.controller;

import com.eleclib.dto.BookCardDto;
import com.eleclib.dto.ContinueReadingDto;
import com.eleclib.dto.LibraryActivityDto;
import com.eleclib.dto.LibraryProgressDto;
import com.eleclib.model.Book;
import com.eleclib.model.BookMark;
import com.eleclib.model.ReadingNote;
import com.eleclib.model.User;
import com.eleclib.service.BookMarkService;
import com.eleclib.service.BookService;
import com.eleclib.service.ContinueReadingService;
import com.eleclib.service.FavoriteService;
import com.eleclib.service.ReadingNoteService;
import com.eleclib.service.ReadingShelfService;
import com.eleclib.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/my-library")
@RequiredArgsConstructor
public class MyLibraryController {

    private static final int SECTION_LIMIT = 6;
    private static final int ACTIVITY_LIMIT = 8;

    private final UserService userService;
    private final ContinueReadingService continueReadingService;
    private final FavoriteService favoriteService;
    private final ReadingShelfService readingShelfService;
    private final BookMarkService bookMarkService;
    private final ReadingNoteService readingNoteService;
    private final BookService bookService;

    @GetMapping
    public String index(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";

        List<ContinueReadingDto> progress = continueReadingService.getInProgress(user, SECTION_LIMIT);
        Map<Long, BookCardDto> progressCards = bookService.findCardsByBookIdsInOrder(
                        progress.stream().map(ContinueReadingDto::getBookId).toList(), user)
                .stream().collect(Collectors.toMap(BookCardDto::getBookId, Function.identity(), (a, b) -> a));
        List<LibraryProgressDto> inProgress = progress.stream()
                .map(item -> {
                    BookCardDto card = progressCards.get(item.getBookId());
                    if (card == null) return null;
                    int percent = item.getTotalPages() <= 0 ? 0
                            : Math.min(100, (int) Math.round(100.0 * item.getLastPage() / item.getTotalPages()));
                    return new LibraryProgressDto(card, item.getLastPage(), item.getTotalPages(), percent);
                })
                .filter(Objects::nonNull)
                .toList();

        List<BookCardDto> favorites = bookService.findCardsByBookIdsInOrder(
                favoriteService.getFavoriteBookIds(user.getUserId(), SECTION_LIMIT), user);

        List<BookMark> bookmarks = bookMarkService.getLatestForUser(user.getUserId(), ACTIVITY_LIMIT);
        List<ReadingNote> notes = readingNoteService.getLatestForUser(user.getUserId(), ACTIVITY_LIMIT);
        List<Long> activityBookIds = new ArrayList<>();
        bookmarks.stream().map(BookMark::getBookId).filter(Objects::nonNull).forEach(activityBookIds::add);
        notes.stream().map(ReadingNote::getBookId).filter(Objects::nonNull).forEach(activityBookIds::add);
        Map<Long, Book> activityBooks = new HashMap<>();
        for (Long id : activityBookIds.stream().distinct().toList()) {
            Book book = bookService.findById(id);
            if (book != null) activityBooks.put(id, book);
        }
        List<LibraryActivityDto> activities = new ArrayList<>();
        bookmarks.forEach(mark -> {
            Book book = activityBooks.get(mark.getBookId());
            if (book != null) activities.add(new LibraryActivityDto(
                    "Закладка", mark.getId(), book.getBookId(), book.getTitle(), mark.getPageNumber(),
                    mark.getTitle(), null, mark.getCreatedAt()));
        });
        notes.forEach(note -> {
            Book book = activityBooks.get(note.getBookId());
            if (book != null) activities.add(new LibraryActivityDto(
                    "Заметка", note.getId(), book.getBookId(), book.getTitle(), note.getPageNumber(),
                    note.getTitle(), note.getQuoteText(), note.getCreatedAt()));
        });
        activities.sort(Comparator.comparing(LibraryActivityDto::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        model.addAttribute("inProgress", inProgress);
        model.addAttribute("favorites", favorites);
        model.addAttribute("shelves", readingShelfService.listSummaries(user.getUserId()));
        model.addAttribute("activities", activities.stream().limit(ACTIVITY_LIMIT).toList());
        model.addAttribute("pageTitle", "Моя библиотека");
        return "my-library/index";
    }

    @PostMapping("/bookmarks/{bookId}/{bookmarkId}/rename")
    public String renameBookmark(@PathVariable Long bookId, @PathVariable Long bookmarkId,
                                 @RequestParam(required = false) String title) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        bookMarkService.renameBookmark(user.getUserId(), bookId, bookmarkId, title);
        return "redirect:/my-library";
    }

    @PostMapping("/bookmarks/{bookId}/{bookmarkId}/delete")
    public String deleteBookmark(@PathVariable Long bookId, @PathVariable Long bookmarkId) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        bookMarkService.deleteBookmark(user.getUserId(), bookmarkId);
        return "redirect:/my-library";
    }

    @PostMapping("/notes/{bookId}/{noteId}/rename")
    public String renameNote(@PathVariable Long bookId, @PathVariable Long noteId,
                             @RequestParam(required = false) String title) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        readingNoteService.renameNote(user.getUserId(), bookId, noteId, title);
        return "redirect:/my-library";
    }

    @PostMapping("/notes/{bookId}/{noteId}/delete")
    public String deleteNote(@PathVariable Long bookId, @PathVariable Long noteId) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        readingNoteService.deleteNote(user.getUserId(), noteId);
        return "redirect:/my-library";
    }
}
