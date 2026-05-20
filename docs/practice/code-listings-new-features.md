# Листинги кода — новые функции ElecLib

Фрагменты ключевой логики трёх функций практики: **обложки**, **комиксы (PDF)**, **оглавление**.

---

## 1. Оглавление в текстовой читалке

### Листинг 1 — построение оглавления (`ReaderToc.build`)

```java
public static List<ReaderTocEntryDto> build(String text) {
    if (text == null || text.isBlank()) {
        return List.of();
    }
    List<String> pages = ReaderPaging.splitIntoPages(text);
    List<ReaderTocEntryDto> entries = new ArrayList<>();
    int lineStart = 0;
    int i = 0;
    while (i < text.length() && entries.size() < MAX_ENTRIES) {
        int lineEnd = text.indexOf('\n', i);
        if (lineEnd < 0) lineEnd = text.length();
        String trimmed = text.substring(i, lineEnd).trim();
        if (!trimmed.isEmpty() && trimmed.length() <= MAX_LINE_LEN
                && matchesTocLine(trimmed)) {
            String title = normalizeTitle(trimmed);
            if (!title.isEmpty()) {
                int page = pageForCharOffset(pages, lineStart);
                entries.add(ReaderTocEntryDto.builder()
                        .title(title).pageNumber(page).build());
            }
        }
        lineStart = lineEnd < text.length() ? lineEnd + 1 : lineEnd;
        i = lineEnd < text.length() ? lineEnd + 1 : lineEnd;
    }
    return dedupe(entries);
}
```

*Источник: `src/main/java/com/eleclib/util/ReaderToc.java`*

### Листинг 2 — разбиение текста на страницы (`ReaderPaging`)

```java
public static final int CHARS_PER_PAGE = 2500;

public static List<String> splitIntoPages(String text) {
    if (text == null || text.isBlank()) {
        return List.of("");
    }
    List<String> result = new ArrayList<>();
    int pos = 0;
    while (pos < text.length()) {
        int end = Math.min(pos + CHARS_PER_PAGE, text.length());
        String chunk = text.substring(pos, end);
        if (end < text.length()) {
            int breakAt = Math.max(chunk.lastIndexOf(' '), chunk.lastIndexOf('\n'));
            if (breakAt > CHARS_PER_PAGE / 2) {
                chunk = chunk.substring(0, breakAt + 1);
                end = pos + breakAt + 1;
            }
        }
        result.add(chunk);
        pos = end;
    }
    return result;
}
```

*Источник: `src/main/java/com/eleclib/util/ReaderPaging.java`*

### Листинг 3 — передача оглавления в читалку (`BookController.read`)

```java
if (book.isComic()) {
    if (!comicPdfStorageService.hasPdf(id)) {
        return "redirect:/books/" + id + "?comicMissing=1";
    }
    return "books/read-comic";
}
model.addAttribute("tocEntries", ReaderToc.build(book.getText()));
return "books/read";
```

*Источник: `src/main/java/com/eleclib/controller/BookController.java`*

### Листинг 4 — разметка панели оглавления (`read.html`)

```html
<div class="reader-toc reader-toc-panel" id="readerToc" aria-hidden="true">
    <h3>Оглавление</h3>
    <ul id="tocList" class="toc-list">
        <li th:each="e : ${tocEntries}" class="toc-item"
            th:attr="data-page=${e.pageNumber}">
            <span class="toc-title" th:text="${e.title}">Глава 1</span>
            <button type="button" class="btn small toc-go">Перейти</button>
        </li>
    </ul>
</div>
```

*Источник: `src/main/resources/templates/books/read.html`*

---

## 2. Комиксы (PDF)

### Листинг 5 — сохранение и выдача PDF (`ComicPdfStorageService`)

```java
public void store(Long bookId, MultipartFile file) throws IOException {
    if (bookId == null || bookId <= 0) {
        throw new IllegalArgumentException("Некорректный id книги");
    }
    if (file == null || file.isEmpty()) {
        throw new IllegalArgumentException("Файл PDF не выбран");
    }
    Files.copy(file.getInputStream(), pathFor(bookId), StandardCopyOption.REPLACE_EXISTING);
}

public Resource asResource(Long bookId) {
    Path path = pathFor(bookId);
    if (!Files.isRegularFile(path)) {
        return null;
    }
    return new FileSystemResource(path);
}
```

*Источник: `src/main/java/com/eleclib/service/ComicPdfStorageService.java`*

### Листинг 6 — отдача PDF клиенту (`BookController.comicPdf`)

```java
@GetMapping("/{id}/comic.pdf")
public ResponseEntity<Resource> comicPdf(@PathVariable Long id) {
    User user = userService.getCurrentUser();
    if (user == null || !bookService.hasSubscriptionAccess(user)) {
        return ResponseEntity.status(403).build();
    }
    Book book = bookService.findById(id);
    if (book == null || !book.isComic()) {
        return ResponseEntity.notFound().build();
    }
    Resource resource = comicPdfStorageService.asResource(id);
    if (resource == null) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"comic-" + id + ".pdf\"")
            .body(resource);
}
```

*Источник: `src/main/java/com/eleclib/controller/BookController.java`*

### Листинг 7 — каталог комиксов (`BookService.findComicsAsCards`)

```java
public List<BookCardDto> findComicsAsCards(User currentUser) {
    return toCardDtos(
            bookRepository.findAll().stream().filter(Book::isComic).toList(),
            currentUser);
}
```

*Источник: `src/main/java/com/eleclib/service/BookService.java`*

---

## 3. Обложки в каталоге

### Листинг 8 — формирование карточки с обложкой и признаками комикса (`BookService.toCardDtos`)

```java
return BookCardDto.builder()
        .bookId(b.getBookId())
        .title(b.getTitle())
        .coverUrl(b.getCoverUrl())
        .subscriptionRequired(!hasSubscription)
        .comic(b.isComic())
        .comicReady(b.isComic() && comicPdfStorageService.hasPdf(b.getBookId()))
        .build();
```

*Источник: `src/main/java/com/eleclib/service/BookService.java` (фрагмент метода `toCardDtos`)*

### Листинг 9 — вывод обложки или заглушки (`book-card.html`)

```html
<img th:if="${book.coverUrl != null && !#strings.isEmpty(book.coverUrl)}"
     class="book-cover-img"
     th:src="${book.coverUrl}"
     th:alt="|Обложка: ${book.title}|"
     loading="lazy"/>
<div th:if="${book.coverUrl == null || #strings.isEmpty(book.coverUrl)}"
     class="book-cover-placeholder" aria-hidden="true">
    <span class="book-cover-letter"
          th:text="${#strings.substring(book.title, 0, 1)}">А</span>
</div>
```

*Источник: `src/main/resources/templates/fragments/book-card.html`*

### Листинг 10 — тип контента «комикс» в модели (`Book`)

```java
private String contentType;

@JsonIgnore
public boolean isComic() {
    return CONTENT_TYPE_COMIC.equalsIgnoreCase(contentType);
}

public static final String CONTENT_TYPE_COMIC = "comic";
public static final String CONTENT_TYPE_TEXT = "text";
```

*Источник: `src/main/java/com/eleclib/model/Book.java`*

---

## 4. JUnit-тесты новых функций

### Листинг 11 — тест оглавления (`ReaderTocTest`)

```java
@Test
void build_mapsHeadingsToPages() {
    StringBuilder sb = new StringBuilder();
    sb.append("Глава 1\n");
    sb.append("x".repeat(3000)).append("\n");
    sb.append("Глава 2\n");
    sb.append("y".repeat(500));
    List<ReaderTocEntryDto> toc = ReaderToc.build(sb.toString());
    assertEquals(2, toc.size());
    assertEquals("Глава 1", toc.get(0).getTitle());
    assertEquals(1, toc.get(0).getPageNumber());
    assertEquals("Глава 2", toc.get(1).getTitle());
    assertTrue(toc.get(1).getPageNumber() >= 2);
}

@Test
void matchesTocLine_recognizesCommonHeadings() {
    assertTrue(ReaderToc.matchesTocLine("Глава 1"));
    assertTrue(ReaderToc.matchesTocLine("§ 3 Начало"));
    assertFalse(ReaderToc.matchesTocLine("Обычный абзац без структуры."));
}
```

*Источник: `src/test/java/com/eleclib/util/ReaderTocTest.java`*

### Листинг 12 — тест постраничности (`ReaderPagingTest`)

```java
@Test
void totalPages_longText_isAtLeastTwo() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 80; i++) {
        sb.append("word").append(i).append(' ');
        sb.append("x".repeat(120));
        sb.append(' ');
    }
    assertTrue(ReaderPaging.totalPages(sb.toString()) >= 2);
}

@Test
void totalPages_emptyText_isOne() {
    assertEquals(1, ReaderPaging.totalPages(""));
    assertEquals(1, ReaderPaging.totalPages("   "));
}
```

*Источник: `src/test/java/com/eleclib/util/ReaderPagingTest.java`*

### Листинг 13 — тест хранения PDF (`ComicPdfStorageServiceTest`)

```java
@TempDir
Path tempDir;

@BeforeEach
void setUp() throws Exception {
    service = new ComicPdfStorageService();
    ReflectionTestUtils.setField(service, "comicsDirProperty", tempDir.toString());
    service.init();
}

@Test
void hasPdf_trueAfterStore() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
            "pdfFile", "comic.pdf", "application/pdf", "%PDF-1.4\n".getBytes());
    service.store(7L, file);
    assertTrue(service.hasPdf(7L));
    assertTrue(Files.exists(service.pathFor(7L)));
}

@Test
void store_rejectsNonPdf() {
    MockMultipartFile file = new MockMultipartFile(
            "pdfFile", "notes.txt", "text/plain", "hello".getBytes());
    assertThrows(IllegalArgumentException.class, () -> service.store(1L, file));
}
```

*Источник: `src/test/java/com/eleclib/service/ComicPdfStorageServiceTest.java`*

### Листинг 14 — тест карточек: обложка и комикс (`BookServiceCardFeaturesTest`)

```java
@ExtendWith(MockitoExtension.class)
class BookServiceCardFeaturesTest {

    @Mock ComicPdfStorageService comicPdfStorageService;
    @InjectMocks BookService bookService;

    @Test
    void findBookCardById_mapsCoverUrl() {
        Book book = Book.builder()
                .bookId(5L).coverUrl("https://cdn.example/cover.png")
                .contentType(Book.CONTENT_TYPE_TEXT)
                .authorId(10L).genreId(20L).build();
        when(bookRepository.findById(5L)).thenReturn(Optional.of(book));

        BookCardDto card = bookService.findBookCardById(5L, subscriber);

        assertEquals("https://cdn.example/cover.png", card.getCoverUrl());
        assertFalse(card.isComic());
    }

    @Test
    void findBookCardById_comicReadyWhenPdfOnDisk() {
        when(bookRepository.findById(8L)).thenReturn(Optional.of(comic));
        when(comicPdfStorageService.hasPdf(8L)).thenReturn(true);

        BookCardDto card = bookService.findBookCardById(8L, subscriber);

        assertTrue(card.isComic());
        assertTrue(card.isComicReady());
    }
}
```

*Источник: `src/test/java/com/eleclib/service/BookServiceCardFeaturesTest.java`*

### Листинг 15 — тест модели `Book` (`BookComicAndCoverTest`)

```java
@Test
void isComic_trueWhenContentTypeComic() {
    Book book = Book.builder().contentType(Book.CONTENT_TYPE_COMIC).build();
    assertTrue(book.isComic());
}

@Test
void coverUrl_storedOnBuilder() {
    Book book = Book.builder()
            .title("Тест")
            .coverUrl("https://example.com/cover.jpg")
            .build();
    assertEquals("https://example.com/cover.jpg", book.getCoverUrl());
}
```

*Источник: `src/test/java/com/eleclib/model/BookComicAndCoverTest.java`*

---

## Сводка листингов

| Листинг | Раздел | Назначение |
|---------|--------|------------|
| 1–2 | Оглавление | Поиск заголовков и нумерация страниц |
| 3–4 | Оглавление | Контроллер и UI панели TOC |
| 5–7 | Комиксы | Хранение PDF, HTTP-выдача, фильтр каталога |
| 8–10 | Обложки + комикс | DTO карточки, шаблон, поле `content_type` |
| 11–12 | Тесты | `ReaderTocTest`, `ReaderPagingTest` |
| 13 | Тесты | `ComicPdfStorageServiceTest` |
| 14–15 | Тесты | `BookServiceCardFeaturesTest`, `BookComicAndCoverTest` |
