package com.eleclib.service;

import com.eleclib.model.*;
import com.eleclib.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReadingShelfServiceTest {
    ReadingShelfRepository shelves;
    ReadingShelfBookRepository books;
    ReadingShelfService service;
    List<ReadingShelf> defaults;
    @BeforeEach void setUp() {
        shelves = mock(ReadingShelfRepository.class); books = mock(ReadingShelfBookRepository.class);
        service = new ReadingShelfService(shelves, books, mock(BookService.class));
        defaults = List.of(shelf(1L, "want"), shelf(2L, "reading"), shelf(3L, "done"), shelf(4L, null));
    }
    ReadingShelf shelf(Long id, String key) {
        return ReadingShelf.builder().shelfId(id).userId(7L).name("Shelf").systemKey(key).build();
    }
    ReadingShelfBook relation(Long id) {
        return ReadingShelfBook.builder().shelfId(id).bookId(12L).build();
    }
    @Test void unfinishedBookMovesToReadingAndKeepsCustomAndForeignShelves() {
        when(shelves.findByUserId(7L)).thenReturn(defaults);
        when(books.findByBookId(12L)).thenReturn(List.of(relation(1L), relation(3L), relation(4L), relation(99L)));
        service.updateReadingSystemShelf(7L, 12L, false);
        verify(books).delete(1L, 12L); verify(books).delete(3L, 12L); verify(books).save(2L, 12L);
        verify(books, never()).delete(4L, 12L); verify(books, never()).delete(99L, 12L);
    }
    @Test void finishedBookMovesToDone() {
        when(shelves.findByUserId(7L)).thenReturn(defaults);
        when(books.findByBookId(12L)).thenReturn(List.of(relation(2L)));
        service.updateReadingSystemShelf(7L, 12L, true);
        verify(books).delete(2L, 12L); verify(books).save(3L, 12L);
    }
    @Test void repeatedUpdateDoesNotDuplicateShelfEntry() {
        when(shelves.findByUserId(7L)).thenReturn(defaults);
        when(books.findByBookId(12L)).thenReturn(List.of(relation(3L)));
        service.updateReadingSystemShelf(7L, 12L, true);
        verify(books, never()).save(anyLong(), anyLong());
    }
    @Test void missingUserCannotChangeShelves() {
        service.updateReadingSystemShelf(null, 12L, true);
        verifyNoInteractions(shelves, books);
    }
    @Test void cannotRemoveBookFromAnotherUsersShelf() {
        when(shelves.findById(9L)).thenReturn(ReadingShelf.builder().shelfId(9L).userId(99L).build());
        assertThrows(IllegalArgumentException.class, () -> service.removeBookFromShelf(7L, 9L, 12L));
        verifyNoInteractions(books);
    }
    @Test void customShelfNameIsTrimmed() {
        service.createCustomShelf(7L, "  Summer  ");
        verify(shelves).save(argThat(s -> s.getUserId().equals(7L) && s.getName().equals("Summer") && s.getSystemKey() == null));
    }
    @Test void blankShelfNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createCustomShelf(7L, "  "));
        verifyNoInteractions(shelves);
    }
}
