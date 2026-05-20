package com.eleclib.controller;

import com.eleclib.dto.BookCardDto;
import com.eleclib.model.Book;
import com.eleclib.model.User;
import com.eleclib.model.BookMark;
import com.eleclib.model.ReadingNote;
import com.eleclib.service.BookMarkService;
import com.eleclib.service.BookRatingService;
import com.eleclib.service.BookService;
import com.eleclib.service.ContinueReadingService;
import com.eleclib.service.FavoriteService;
import com.eleclib.service.ReadingNoteService;
import com.eleclib.service.ReadingPositionService;
import com.eleclib.service.ReadingShelfService;
import com.eleclib.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final UserService userService;
    private final FavoriteService favoriteService;
    private final BookRatingService bookRatingService;
    private final ReadingPositionService readingPositionService;
    private final BookMarkService bookMarkService;
    private final ReadingNoteService readingNoteService;
    private final ContinueReadingService continueReadingService;
    private final ReadingShelfService readingShelfService;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long genreId,
                       Model model) {
        User user = userService.getCurrentUser();
        List<BookCardDto> books;
        if (q != null && !q.isBlank()) {
            books = bookService.searchByTitleOrAuthor(q, user);
        } else if (genreId != null) {
            books = bookService.findByGenre(genreId, user);
        } else {
            books = bookService.findAllAsCards(user);
        }
        model.addAttribute("books", books);
        model.addAttribute("searchQuery", q != null ? q : "");
        List<BookCardDto> recommended = user != null ? bookService.getRecommendedBooks(user, 12) : List.of();
        if ((q != null && !q.isBlank()) || genreId != null) {
            List<Long> bookIdsInList = books.stream().map(BookCardDto::getBookId).toList();
            recommended = recommended.stream().filter(r -> !bookIdsInList.contains(r.getBookId())).collect(Collectors.toList());
        }
        recommended = recommended.stream().limit(8).collect(Collectors.toList());
        model.addAttribute("recommendedBooks", recommended);
        if (user != null) {
            continueReadingService.getBanner(user).ifPresent(dto -> model.addAttribute("continueReading", dto));
        }
        return "books/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        User user = userService.getCurrentUser();
        Book book = bookService.findById(id);
        if (book == null) {
            return "redirect:/books";
        }
        BookCardDto card = bookService.findBookCardById(id, user);
        model.addAttribute("book", book);
        model.addAttribute("card", card);
        model.addAttribute("reviews", bookService.getReviews(id, user != null ? user.getUserId() : null));
        model.addAttribute("hasAccess", user != null && bookService.hasSubscriptionAccess(user));
        if (user != null) {
            model.addAttribute("myRating", bookRatingService.getRating(user.getUserId(), id));
            model.addAttribute("shelfSummaries", readingShelfService.listSummaries(user.getUserId()));
            model.addAttribute("shelfIdsForBook", readingShelfService.shelfIdsContainingBook(user.getUserId(), id));
        } else {
            model.addAttribute("shelfSummaries", List.of());
            model.addAttribute("shelfIdsForBook", java.util.Set.of());
        }
        return "books/detail";
    }

    @GetMapping("/{id}/read")
    public String read(@PathVariable Long id, Model model) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        if (!bookService.hasSubscriptionAccess(user)) {
            return "redirect:/subscription?required=1";
        }
        Book book = bookService.findById(id);
        if (book == null) {
            return "redirect:/books";
        }
        int lastPage = readingPositionService.getLastPage(user.getUserId(), id);
        model.addAttribute("book", book);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("bookMarks", bookMarkService.getBookmarks(user.getUserId(), id));
        model.addAttribute("readingNotes", readingNoteService.getNotes(user.getUserId(), id));
        model.addAttribute("inFavorites", favoriteService.isFavorite(user.getUserId(), id));
        return "books/read";
    }

    @PostMapping("/{id}/reading-position")
    @ResponseBody
    public void saveReadingPosition(@PathVariable Long id, @RequestParam int page) {
        User user = userService.getCurrentUser();
        if (user != null && page >= 1) {
            readingPositionService.saveLastPage(user.getUserId(), id, page);
        }
    }

    @PostMapping("/{id}/bookmarks")
    @ResponseBody
    public BookMark addBookmark(@PathVariable Long id,
                                @RequestParam int page,
                                @RequestParam(required = false) String title) {
        User user = userService.getCurrentUser();
        if (user == null || page < 1) return null;
        return bookMarkService.addBookmark(user.getUserId(), id, page, title);
    }

    @PostMapping("/{id}/bookmarks/rename")
    @ResponseBody
    public BookMark renameBookmark(@PathVariable Long id,
                                   @RequestParam Long bookmarkId,
                                   @RequestParam(required = false) String title) {
        User user = userService.getCurrentUser();
        if (user == null) return null;
        return bookMarkService.renameBookmark(user.getUserId(), id, bookmarkId, title);
    }

    @PostMapping("/{id}/bookmarks/delete")
    @ResponseBody
    public void deleteBookmark(@PathVariable Long id, @RequestParam Long bookmarkId) {
        User user = userService.getCurrentUser();
        if (user != null) {
            bookMarkService.deleteBookmark(user.getUserId(), bookmarkId);
        }
    }

    @PostMapping("/{id}/notes")
    @ResponseBody
    public ReadingNote addReadingNote(@PathVariable Long id,
                                      @RequestParam int page,
                                      @RequestParam String quote,
                                      @RequestParam(required = false) String title) {
        User user = userService.getCurrentUser();
        if (user == null || page < 1) return null;
        return readingNoteService.addNote(user.getUserId(), id, page, quote, title);
    }

    @PostMapping("/{id}/notes/rename")
    @ResponseBody
    public ReadingNote renameReadingNote(@PathVariable Long id,
                                         @RequestParam Long noteId,
                                         @RequestParam(required = false) String title) {
        User user = userService.getCurrentUser();
        if (user == null) return null;
        return readingNoteService.renameNote(user.getUserId(), id, noteId, title);
    }

    @PostMapping("/{id}/notes/delete")
    @ResponseBody
    public void deleteReadingNote(@PathVariable Long id, @RequestParam Long noteId) {
        User user = userService.getCurrentUser();
        if (user != null) {
            readingNoteService.deleteNote(user.getUserId(), noteId);
        }
    }

    @PostMapping("/{id}/favorite")
    public String toggleFavorite(@PathVariable Long id,
                                @RequestParam(defaultValue = "0") int add,
                                @RequestParam(required = false) String returnTo) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        if (add == 1) {
            favoriteService.addFavorite(user.getUserId(), id);
        } else {
            favoriteService.removeFavorite(user.getUserId(), id);
        }
        if ("favorites".equals(returnTo)) return "redirect:/favorites";
        if ("read".equals(returnTo)) return "redirect:/books/" + id + "/read";
        return "redirect:/books/" + id;
    }

    @PostMapping("/{id}/review")
    public String submitReview(@PathVariable Long id,
                              @RequestParam int rating,
                              @RequestParam(required = false) String review,
                              RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        try {
            bookRatingService.saveRating(user.getUserId(), id, rating, review);
            redirectAttributes.addFlashAttribute("message", "Отзыв сохранён.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Не удалось сохранить оценку: " + e.getMessage());
        }
        return "redirect:/books/" + id;
    }

    @PostMapping("/{id}/review/delete")
    public String deleteReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        try {
            bookRatingService.deleteRating(user.getUserId(), id);
            redirectAttributes.addFlashAttribute("message", "Отзыв удалён.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Не удалось удалить отзыв: " + e.getMessage());
        }
        return "redirect:/books/" + id;
    }
}
