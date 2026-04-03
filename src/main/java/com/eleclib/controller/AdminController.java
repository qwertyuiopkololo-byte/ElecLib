package com.eleclib.controller;

import com.eleclib.model.Author;
import com.eleclib.model.Book;
import com.eleclib.model.Genre;
import com.eleclib.model.Subscription;
import com.eleclib.model.User;
import com.eleclib.repository.AuthorRepository;
import com.eleclib.repository.BookRepository;
import com.eleclib.repository.GenreRepository;
import com.eleclib.repository.SubscriptionRepository;
import com.eleclib.repository.UserRepository;
import com.eleclib.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final BookRepository bookRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("usersCount", userRepository.count());
        model.addAttribute("booksCount", bookRepository.count());
        model.addAttribute("authorsCount", authorRepository.count());
        model.addAttribute("genresCount", genreRepository.count());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    @GetMapping("/authors")
    public String authors(Model model) {
        model.addAttribute("authors", authorRepository.findAll());
        return "admin/authors";
    }

    @GetMapping("/authors/new")
    public String newAuthorForm(Model model) {
        model.addAttribute("author", new Author());
        return "admin/author-form";
    }

    @PostMapping("/authors")
    public String saveAuthor(@ModelAttribute Author author, RedirectAttributes ra) {
        authorRepository.save(author);
        ra.addFlashAttribute("message", "Автор сохранён");
        return "redirect:/admin/authors";
    }

    @GetMapping("/authors/edit/{id}")
    public String editAuthor(@PathVariable Long id, Model model) {
        model.addAttribute("author", authorRepository.findById(id).orElseThrow());
        return "admin/author-form";
    }

    @GetMapping("/genres")
    public String genres(Model model) {
        model.addAttribute("genres", genreRepository.findAll());
        return "admin/genres";
    }

    @GetMapping("/genres/new")
    public String newGenreForm(Model model) {
        model.addAttribute("genre", new Genre());
        return "admin/genre-form";
    }

    @PostMapping("/genres")
    public String saveGenre(@ModelAttribute Genre genre, RedirectAttributes ra) {
        genreRepository.save(genre);
        ra.addFlashAttribute("message", "Жанр сохранён");
        return "redirect:/admin/genres";
    }

    @GetMapping("/genres/edit/{id}")
    public String editGenre(@PathVariable Long id, Model model) {
        model.addAttribute("genre", genreRepository.findById(id).orElseThrow());
        return "admin/genre-form";
    }

    @GetMapping("/books")
    public String books(Model model) {
        model.addAttribute("books", bookRepository.findAll());
        model.addAttribute("authorMap", authorRepository.findAll().stream().collect(Collectors.toMap(Author::getAuthorId, a -> a)));
        model.addAttribute("genreMap", genreRepository.findAll().stream().collect(Collectors.toMap(Genre::getGenreId, g -> g)));
        return "admin/books";
    }

    @GetMapping("/books/new")
    public String newBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("authors", authorRepository.findAll());
        model.addAttribute("genres", genreRepository.findAll());
        return "admin/book-form";
    }

    @PostMapping("/books")
    public String saveBook(@ModelAttribute Book book,
                           @RequestParam Long authorId,
                           @RequestParam Long genreId,
                           RedirectAttributes ra) {
        book.setAuthorId(authorId);
        book.setGenreId(genreId);
        bookRepository.save(book);
        ra.addFlashAttribute("message", "Книга сохранена");
        return "redirect:/admin/books";
    }

    @GetMapping("/books/edit/{id}")
    public String editBook(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookRepository.findById(id).orElseThrow());
        model.addAttribute("authors", authorRepository.findAll());
        model.addAttribute("genres", genreRepository.findAll());
        return "admin/book-form";
    }

    @GetMapping("/subscriptions")
    public String subscriptions(Model model) {
        List<Subscription> subs = subscriptionRepository.findAll();
        model.addAttribute("subscriptions", subs);
        model.addAttribute("userLoginMap", subs.stream().map(Subscription::getUserId).distinct()
                .collect(Collectors.toMap(id -> id, id -> userRepository.findById(id).map(User::getLogin).orElse("?"))));
        return "admin/subscriptions";
    }

    @PostMapping("/subscriptions/activate")
    public String activateForUser(@RequestParam Long userId, @RequestParam int months, RedirectAttributes ra) {
        userRepository.findById(userId).orElseThrow();
        Subscription sub = Subscription.builder()
                .userId(userId)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(months))
                .status("active")
                .build();
        subscriptionRepository.save(sub);
        ra.addFlashAttribute("message", "Подписка активирована для пользователя");
        return "redirect:/admin/subscriptions";
    }
}
