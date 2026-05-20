package com.eleclib.controller;

import com.eleclib.dto.BookCardDto;
import com.eleclib.model.User;
import com.eleclib.service.BookService;
import com.eleclib.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/comics")
@RequiredArgsConstructor
public class ComicController {

    private final BookService bookService;
    private final UserService userService;

    @GetMapping
    public String list(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        List<BookCardDto> comics = bookService.findComicsAsCards(user);
        model.addAttribute("books", comics);
        model.addAttribute("recommendedBooks", bookService.getRecommendedComics(user, 8));
        return "comics/list";
    }
}
