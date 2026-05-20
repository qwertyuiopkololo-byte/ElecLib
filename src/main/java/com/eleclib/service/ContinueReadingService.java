package com.eleclib.service;

import com.eleclib.dto.ContinueReadingDto;
import com.eleclib.model.Book;
import com.eleclib.model.ReadingPosition;
import com.eleclib.model.User;
import com.eleclib.repository.ReadingPositionRepository;
import com.eleclib.util.ReaderPaging;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContinueReadingService {

    private final ReadingPositionRepository readingPositionRepository;
    private final BookService bookService;

    /**
     * Последняя активная книга с незавершённым чтением (in-app баннер).
     */
    public Optional<ContinueReadingDto> getBanner(User user) {
        if (user == null) {
            return Optional.empty();
        }
        List<ReadingPosition> positions = readingPositionRepository.findByUserId(user.getUserId());
        if (positions.isEmpty()) {
            return Optional.empty();
        }
        Comparator<ReadingPosition> byUpdated = Comparator
                .comparing(ReadingPosition::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        return positions.stream()
                .max(byUpdated)
                .flatMap(this::toDtoIfIncomplete);
    }

    private Optional<ContinueReadingDto> toDtoIfIncomplete(ReadingPosition pos) {
        Book book = bookService.findById(pos.getBookId());
        if (book == null) {
            return Optional.empty();
        }
        int total = ReaderPaging.totalPages(book.getText());
        int last = pos.getLastPage() != null ? pos.getLastPage() : 1;
        if (last >= total) {
            return Optional.empty();
        }
        return Optional.of(ContinueReadingDto.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .lastPage(last)
                .totalPages(total)
                .build());
    }
}
