package com.eleclib.service;

import com.eleclib.model.ReadingPosition;
import com.eleclib.model.Book;
import com.eleclib.repository.ReadingPositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingPositionServiceTest {

    @Mock
    ReadingPositionRepository repository;

    @Mock BookService bookService;
    @Mock ReadingShelfService readingShelfService;
    @Mock MonthlyGoalService monthlyGoalService;

    @InjectMocks
    ReadingPositionService service;

    @Test
    void getLastPage_returns1_whenNoRecord() {
        when(repository.findByUserIdAndBookId(1L, 2L)).thenReturn(Optional.empty());
        assertEquals(1, service.getLastPage(1L, 2L));
    }

    @Test
    void getLastPage_returns1_whenStoredPageInvalid() {
        when(repository.findByUserIdAndBookId(1L, 2L)).thenReturn(Optional.of(
                ReadingPosition.builder().userId(1L).bookId(2L).lastPage(0).updatedAt(Instant.now()).build()
        ));
        assertEquals(1, service.getLastPage(1L, 2L));
    }

    @Test
    void getLastPage_returnsStoredPage_whenValid() {
        when(repository.findByUserIdAndBookId(1L, 2L)).thenReturn(Optional.of(
                ReadingPosition.builder().userId(1L).bookId(2L).lastPage(5).updatedAt(Instant.now()).build()
        ));
        assertEquals(5, service.getLastPage(1L, 2L));
    }

    @Test
    void saveLastPage_deletesExistingAndSavesNew() {
        when(bookService.findById(2L)).thenReturn(Book.builder().bookId(2L).text("x".repeat(25000)).build());
        when(repository.findByUserIdAndBookId(1L, 2L)).thenReturn(Optional.of(
                ReadingPosition.builder().userId(1L).bookId(2L).lastPage(3).updatedAt(Instant.now()).build()
        ));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveLastPage(1L, 2L, 10);

        verify(repository, times(1)).deleteByUserIdAndBookId(1L, 2L);
        verify(repository, times(1)).save(any(ReadingPosition.class));
    }

    @Test
    void saveLastPage_doesNothing_whenPageLessThan1() {
        service.saveLastPage(1L, 2L, 0);
        verifyNoInteractions(repository);
    }

    @Test void intermediatePageUpdatesReadingShelfButNotMonthlyGoal() throws Exception {
        when(bookService.findById(2L)).thenReturn(Book.builder().bookId(2L).text("x".repeat(5000)).build());
        service.saveLastPage(1L, 2L, 1);
        verify(repository).save(argThat(p -> p.getLastPage() == 1));
        verify(readingShelfService).updateReadingSystemShelf(1L, 2L, false);
        verifyNoInteractions(monthlyGoalService);
    }

    @Test void lastPageUpdatesDoneShelfAndMonthlyGoal() throws Exception {
        when(bookService.findById(2L)).thenReturn(Book.builder().bookId(2L).text("x".repeat(5000)).build());
        service.saveLastPage(1L, 2L, 99);
        verify(repository).save(argThat(p -> p.getLastPage() == 2));
        verify(readingShelfService).updateReadingSystemShelf(1L, 2L, true);
        verify(monthlyGoalService).recordFinished(1L, 2L);
    }

    @Test void missingBookDoesNotCreateProgressOrCompletion() {
        service.saveLastPage(1L, 404L, 3);
        verifyNoInteractions(repository, readingShelfService, monthlyGoalService);
    }

    @Test void comicUsesActualClientPageCount() throws Exception {
        when(bookService.findById(2L)).thenReturn(Book.builder().bookId(2L).contentType(Book.CONTENT_TYPE_COMIC).build());
        service.saveLastPage(1L, 2L, 12, 10);
        verify(repository).save(argThat(p -> p.getLastPage() == 10));
        verify(monthlyGoalService).recordFinished(1L, 2L);
    }

    @Test void comicWithoutPageCountDoesNotGuessCompletion() {
        when(bookService.findById(2L)).thenReturn(Book.builder().bookId(2L).contentType(Book.CONTENT_TYPE_COMIC).build());
        service.saveLastPage(1L, 2L, 3);
        verify(repository).save(argThat(p -> p.getLastPage() == 3));
        verifyNoInteractions(readingShelfService, monthlyGoalService);
    }
}

