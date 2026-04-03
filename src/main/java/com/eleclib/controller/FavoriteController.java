package com.eleclib.controller;

import com.eleclib.dto.BookCardDto;
import com.eleclib.model.Book;
import com.eleclib.model.User;
import com.eleclib.service.BookService;
import com.eleclib.service.FavoriteService;
import com.eleclib.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final BookService bookService;
    private final UserService userService;

    @GetMapping
    public String list(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        List<Book> favBooks = favoriteService.getFavoriteBooks(user.getUserId());
        List<BookCardDto> cards = bookService.findAllAsCards(user).stream()
                .filter(c -> favBooks.stream().anyMatch(b -> b.getBookId().equals(c.getBookId())))
                .collect(Collectors.toList());
        cards = favBooks.stream()
                .map(b -> bookService.findBookCardById(b.getBookId(), user))
                .collect(Collectors.toList());
        model.addAttribute("books", cards);
        model.addAttribute("recommendedBooks", bookService.getRecommendedBooks(user, 8));
        return "favorites/list";
    }
}
