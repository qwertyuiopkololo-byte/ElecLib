package com.eleclib.controller;

import com.eleclib.model.User;
import com.eleclib.service.ReadingShelfService;
import com.eleclib.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shelves")
@RequiredArgsConstructor
public class ReadingShelfController {

    private final UserService userService;
    private final ReadingShelfService readingShelfService;

    @GetMapping
    public String list(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("summaries", readingShelfService.listSummaries(user.getUserId()));
        model.addAttribute("pageTitle", "Мои полки");
        return "shelves/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        return readingShelfService.findShelfIfOwned(id, user.getUserId())
                .map(shelf -> {
                    model.addAttribute("shelf", shelf);
                    model.addAttribute("books", readingShelfService.listBooksOnShelf(user.getUserId(), id, user));
                    model.addAttribute("pageTitle", shelf.getName());
                    return "shelves/view";
                })
                .orElse("redirect:/shelves");
    }

    @PostMapping("/create")
    public String create(@RequestParam String name, RedirectAttributes ra) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        try {
            readingShelfService.createCustomShelf(user.getUserId(), name);
            ra.addFlashAttribute("message", "Полка создана");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/shelves";
    }

    @PostMapping("/{shelfId}/books/{bookId}")
    public String addBook(@PathVariable Long shelfId,
                          @PathVariable Long bookId,
                          RedirectAttributes ra) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        try {
            readingShelfService.addBookToShelf(user.getUserId(), shelfId, bookId);
            ra.addFlashAttribute("message", "Книга на полке");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/books/" + bookId;
    }

    @PostMapping("/{shelfId}/books/{bookId}/remove")
    public String removeBook(@PathVariable Long shelfId,
                             @PathVariable Long bookId,
                             @RequestParam(required = false) String from,
                             RedirectAttributes ra) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        try {
            readingShelfService.removeBookFromShelf(user.getUserId(), shelfId, bookId);
            ra.addFlashAttribute("message", "Книга убрана с полки");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        if ("shelf".equals(from)) {
            return "redirect:/shelves/" + shelfId;
        }
        return "redirect:/books/" + bookId;
    }
}
