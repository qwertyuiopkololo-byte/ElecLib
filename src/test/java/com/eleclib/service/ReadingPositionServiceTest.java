package com.eleclib.service;

import com.eleclib.model.ReadingPosition;
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
}

