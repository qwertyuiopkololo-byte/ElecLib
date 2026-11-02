package com.eleclib.controller;

import com.eleclib.dto.*;
import com.eleclib.model.*;
import com.eleclib.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyLibraryControllerTest {
    UserService users;
    ContinueReadingService reading;
    FavoriteService favorites;
    ReadingShelfService shelves;
    BookMarkService bookmarks;
    ReadingNoteService notes;
    BookService books;
    MyLibraryController controller;
    User user = User.builder().userId(7L).build();
    @BeforeEach void setUp() {
        users = mock(UserService.class); reading = mock(ContinueReadingService.class); favorites = mock(FavoriteService.class);
        shelves = mock(ReadingShelfService.class); bookmarks = mock(BookMarkService.class);
        notes = mock(ReadingNoteService.class); books = mock(BookService.class);
        controller = new MyLibraryController(users, reading, favorites, shelves, bookmarks, notes, books);
    }
    @Test void guestCannotLoadPersonalData() {
        assertEquals("redirect:/login", controller.index(new ExtendedModelMap()));
        verifyNoInteractions(reading, favorites, shelves, bookmarks, notes, books);
    }
    @Test void emptyLibraryProducesEmptySections() {
        when(users.getCurrentUser()).thenReturn(user);
        var model = new ExtendedModelMap();
        assertEquals("my-library/index", controller.index(model));
        for (String key : List.of("inProgress", "favorites", "shelves", "activities")) assertEquals(List.of(), model.get(key));
    }
    @Test void libraryAggregatesProgressFavoritesAndChronologicalMarks() {
        when(users.getCurrentUser()).thenReturn(user);
        when(reading.getInProgress(user, 6)).thenReturn(List.of(ContinueReadingDto.builder()
                .bookId(12).lastPage(2).totalPages(4).build()));
        var card = BookCardDto.builder().bookId(12L).title("Book").build();
        when(books.findCardsByBookIdsInOrder(List.of(12L), user)).thenReturn(List.of(card));
        when(favorites.getFavoriteBookIds(7L, 6)).thenReturn(List.of(12L));
        when(bookmarks.getLatestForUser(7L, 8)).thenReturn(List.of(BookMark.builder().id(1L).bookId(12L)
                .pageNumber(1).title("Bookmark").createdAt(Instant.parse("2026-11-01T10:00:00Z")).build()));
        when(notes.getLatestForUser(7L, 8)).thenReturn(List.of(ReadingNote.builder().id(2L).bookId(12L)
                .pageNumber(2).quoteText("Quote").createdAt(Instant.parse("2026-11-02T10:00:00Z")).build()));
        when(books.findById(12L)).thenReturn(Book.builder().bookId(12L).title("Book").build());
        var model = new ExtendedModelMap(); controller.index(model);
        var progress = (LibraryProgressDto) ((List<?>) model.get("inProgress")).get(0);
        assertEquals(50, progress.percent()); assertEquals(List.of(card), model.get("favorites"));
        var activities = (List<?>) model.get("activities");
        assertEquals(2L, ((LibraryActivityDto) activities.get(0)).activityId());
        assertEquals(1L, ((LibraryActivityDto) activities.get(1)).activityId());
        verify(books, times(1)).findById(12L);
    }
    @Test void deletedBooksDoNotBreakActivityFeed() {
        when(users.getCurrentUser()).thenReturn(user);
        when(bookmarks.getLatestForUser(7L, 8)).thenReturn(List.of(BookMark.builder().id(1L).bookId(404L).build()));
        var model = new ExtendedModelMap(); controller.index(model);
        assertEquals(List.of(), model.get("activities"));
    }
    @Test void editsAndDeletesUseAuthenticatedUserId() {
        when(users.getCurrentUser()).thenReturn(user);
        assertEquals("redirect:/my-library", controller.renameBookmark(12L, 1L, "New"));
        controller.renameNote(12L, 2L, "Title"); controller.deleteBookmark(12L, 1L); controller.deleteNote(12L, 2L);
        verify(bookmarks).renameBookmark(7L, 12L, 1L, "New"); verify(bookmarks).deleteBookmark(7L, 1L);
        verify(notes).renameNote(7L, 12L, 2L, "Title"); verify(notes).deleteNote(7L, 2L);
    }
}
