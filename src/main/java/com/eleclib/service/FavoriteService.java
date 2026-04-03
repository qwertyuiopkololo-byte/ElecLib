package com.eleclib.service;

import com.eleclib.dto.BookCardDto;
import com.eleclib.model.Book;
import com.eleclib.model.Favorite;
import com.eleclib.repository.BookRepository;
import com.eleclib.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;

    public void addFavorite(Long userId, Long bookId) {
        if (favoriteRepository.existsByUser_UserIdAndBook_BookId(userId, bookId)) return;
        favoriteRepository.save(Favorite.builder().userId(userId).bookId(bookId).build());
    }

    public void removeFavorite(Long userId, Long bookId) {
        favoriteRepository.deleteByUser_UserIdAndBook_BookId(userId, bookId);
    }

    public List<Book> getFavoriteBooks(Long userId) {
        return favoriteRepository.findByUser_UserId(userId).stream()
                .map(f -> bookRepository.findById(f.getBookId()).orElse(null))
                .filter(b -> b != null)
                .collect(Collectors.toList());
    }

    public boolean isFavorite(Long userId, Long bookId) {
        return userId != null && favoriteRepository.existsByUser_UserIdAndBook_BookId(userId, bookId);
    }
}
