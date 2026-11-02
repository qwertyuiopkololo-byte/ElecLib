package com.eleclib.service;

import com.eleclib.model.*;
import com.eleclib.repository.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalMarksTest {
    BookMarkRepository marks = mock(BookMarkRepository.class);
    ReadingNoteRepository notes = mock(ReadingNoteRepository.class);
    BookMarkService bookmarkService = new BookMarkService(marks);
    ReadingNoteService noteService = new ReadingNoteService(notes);

    @Test void anotherUsersBookmarkCannotBeRenamedOrDeleted() {
        when(marks.findById(1L)).thenReturn(BookMark.builder().id(1L).userId(99L).build());
        assertNull(bookmarkService.renameBookmark(7L, 12L, 1L, "Hacked"));
        bookmarkService.deleteBookmark(7L, 1L);
        verify(marks, never()).deleteById(anyLong()); verify(marks, never()).save(any());
    }
    @Test void anotherUsersNoteCannotBeRenamedOrDeleted() {
        when(notes.findById(1L)).thenReturn(ReadingNote.builder().id(1L).userId(99L).build());
        assertNull(noteService.renameNote(7L, 12L, 1L, "Hacked")); noteService.deleteNote(7L, 1L);
        verify(notes, never()).deleteById(anyLong()); verify(notes, never()).save(any());
    }
    @Test void renamedBookmarkPreservesBookPageAndTimestamp() {
        Instant created = Instant.parse("2026-11-01T00:00:00Z");
        when(marks.findById(1L)).thenReturn(BookMark.builder().id(1L).userId(7L).bookId(12L)
                .pageNumber(3).createdAt(created).build());
        bookmarkService.renameBookmark(7L, 999L, 1L, "  New  ");
        verify(marks).save(argThat(m -> m.getTitle().equals("New") && m.getBookId().equals(12L)
                && m.getPageNumber() == 3 && created.equals(m.getCreatedAt())));
    }
    @Test void renamedNotePreservesQuoteAndPosition() {
        when(notes.findById(1L)).thenReturn(ReadingNote.builder().id(1L).userId(7L).bookId(12L)
                .pageNumber(3).quoteText("Original quote").build());
        noteService.renameNote(7L, 999L, 1L, "  Title  ");
        verify(notes).save(argThat(n -> n.getTitle().equals("Title") && n.getBookId().equals(12L)
                && n.getPageNumber() == 3 && n.getQuoteText().equals("Original quote")));
    }
    @Test void emptyQuoteAndInvalidPagesAreRejected() {
        assertNull(noteService.addNote(7L, 12L, 1, "   ", "Title"));
        assertNull(noteService.addNote(7L, 12L, 0, "Quote", "Title"));
        assertNull(bookmarkService.addBookmark(7L, 12L, 0, "Title"));
        verifyNoInteractions(marks, notes);
    }
}
