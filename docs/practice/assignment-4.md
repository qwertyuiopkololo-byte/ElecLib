# Тема 2. Задание 4. Реализация бизнес-логики и слоя данных

В приложении ElecLib реализована бизнес-логика и слой доступа к данным для **трёх новых функций**: чтение комиксов (PDF), оглавление в текстовой читалке и обложки в каталоге. Данные хранятся в **Supabase (PostgreSQL)** через классы пакета `repository`; PDF комиксов — в файловой системе сервера.

Схемы взаимодействия слоёв — в [`figures/eleclib-assignment-4.drawio`](figures/eleclib-assignment-4.drawio). **Диаграмма компонентов по слоям** (как MVVM в мобильном приложении) — в [`figures/eleclib-component-diagram.drawio`](figures/eleclib-component-diagram.drawio), вкладка «Diagramma komponentov».

### Диаграмма компонентов (слои)

На **рисунке 25** представлена **слоистая диаграмма компонентов** по типу мобильного шаблона (View → ViewModel → Model → данные). Для ElecLib слои адаптированы под веб-архитектуру Spring Boot:

| Слой в шаблоне (Android) | Слой в ElecLib |
|--------------------------|----------------|
| View | Thymeleaf-шаблоны (`layout`, `books/list`, `read`, `read-comic`, `book-card`, админ-форма) |
| ViewModel | **Controller** (`BookController`, `AdminController`, `ComicController`) |
| Model / Repository | **Service**, **Repository**, модели и DTO (`Book`, `BookCardDto`, `ReaderToc`) |
| База данных | **Supabase (PostgreSQL)** и каталог **`data/comics/`** для PDF |

Между слоями показаны **двунаправленные связи** (запрос пользователя вниз, ответ HTML/данных вверх).

*Рисунок 25 — Диаграмма компонентов (Draw.io, вкладка «25 - Diagramma komponentov»).*

---

## 1. Комиксы: добавление и чтение PDF

В приложении реализована возможность **добавления и чтения комиксов** в формате PDF.

**Администратор:** в разделе «Админ → Книги» открывает форму, включает «Комикс», при необходимости загружает файл PDF, указывает название, автора, жанр и URL обложки. После нажатия «Сохранить» контроллер **`AdminController.saveBook()`** устанавливает `contentType = comic`, вызывает **`BookRepository.save()`** — запись попадает в таблицу `books`. Затем **`ComicPdfStorageService.store()`** копирует PDF в каталог `data/comics/{book_id}.pdf`.

**Пользователь:** открывает раздел **«Комиксы»** (`ComicController` → **`BookService.findComicsAsCards()`**). Для чтения переходит по ссылке «Читать»; **`BookController.read()`** проверяет вход, подписку и наличие PDF, открывает читалку. Страница PDF запрашивается по `/books/{id}/comic.pdf`. При листании **`ReadingPositionService`** сохраняет номер страницы через **`ReadingPositionRepository`** (таблица `reading_position`).

Для хранения метаданных используется модель **`Book`** (поле `contentType`), для прогресса чтения — **`ReadingPosition`**. Взаимодействие с Supabase — через **`BookRepository`** и **`ReadingPositionRepository`**; файлы PDF — через **`ComicPdfStorageService`**.

*Рисунок 22 — Слои: комикс (админ → БД → диск → читалка) (Draw.io, вкладка «22 - Komiksy logika»).*

### Проверка данных и обработка ошибок (комиксы)

Перед сохранением PDF **`ComicPdfStorageService.store()`** проверяет:

- корректный `bookId`;
- файл выбран и не пустой;
- расширение или MIME-тип указывают на **PDF**.

При нарушении выбрасывается `IllegalArgumentException` с текстом ошибки.

В **`AdminController`**: при ошибке загрузки PDF книга уже сохранена в БД — пользователю показывается flash-сообщение *«Книга сохранена, но PDF не загружен»* и выполняется переход на форму редактирования. Если комикс сохранён без файла — сообщение *«Загрузите PDF при редактировании»*.

При чтении **`BookController.read()`**:

- без авторизации — редирект на `/login`;
- без подписки — на `/subscription`;
- комикс без PDF на диске — редирект на карточку книги с параметром `comicMissing=1`.

**`comicPdf()`** возвращает HTTP 403 без подписки и HTTP 404, если книга не комикс или файл отсутствует.

---

## 2. Поиск книг в каталоге (с обложками)

Реализована функция **поиска и фильтрации** книг в каталоге. Пользователь вводит текст в строку поиска на странице `/books` (параметр `q`) — по **названию** или **имени автора**.

**`BookController.list()`** при непустом `q` вызывает **`BookService.searchByTitleOrAuthor()`**, который обращается к **`BookRepository.searchByTitleOrAuthor()`**: выполняется фильтрация списка книг в памяти (после загрузки из Supabase). Результат преобразуется в **`BookCardDto`** методом **`toCardDtos()`** — в том числе подставляются **`coverUrl`**, признаки `comic` и `comicReady`.

Дополнительно поддерживается фильтр по **жанру** (`genreId`) через **`BookRepository.findByGenre_GenreId()`**.

Обновление списка выполняется **перезагрузкой страницы** с GET-параметрами (веб-приложение); пользователь видит отфильтрованную сетку карточек с обложками.

*Рисунок 23 — Поиск: Controller → Service → BookRepository (Draw.io, вкладка «23 - Poisk»).*

### Проверка данных и обработка ошибок (поиск)

В **`BookRepository.searchByTitleOrAuthor()`** реализована проверка **пустого запроса**: если строка после `trim()` пуста, возвращается **полный список** (`findAll()`).

При отсутствии совпадений шаблон **`books/list.html`** выводит сообщение *«Книг не найдено»* (`th:if` на пустой список `books`).

Ошибки обращения к Supabase обрабатываются на уровне **`SupabaseClient`** (исключения при сетевых сбоях); контроллер в типичном сценарии получает уже загруженный список или пустой результат.

---

## 3. Оглавление и обложки (логика без отдельной таблицы)

### Оглавление

Для **текстовой читалки** оглавление **не хранится в базе данных**. При открытии `/books/{id}/read` контроллер вызывает **`ReaderToc.build(book.getText())`**: утилита сканирует текст, находит строки-заголовки (главы, §, Markdown и др.), с помощью **`ReaderPaging`** вычисляет номер виртуальной страницы и формирует список **`ReaderTocEntryDto`**.

Если текст пустой или заголовки не найдены, возвращается **пустой список** — в интерфейсе показывается *«Заголовки разделов в тексте не найдены»*.

Переход к разделу выполняется на стороне **JavaScript** в шаблоне `read.html` (кнопка «Перейти» у пункта оглавления), без дополнительного запроса к серверу.

*Рисунок 24 — Построение оглавления ReaderToc + ReaderPaging (Draw.io, вкладка «24 - Oglavlenie logika»).*

### Обложки

Поле **`coverUrl`** модели **`Book`** сохраняется в Supabase через **`BookRepository.save()`** при работе админ-формы. **`BookService.toCardDtos()`** копирует URL в DTO; шаблон решает, показать изображение или **заглушку** — отдельной проверки URL на сервере нет (достаточно пустой строки в БД).

Ошибка недоступной картинки по внешней ссылке обрабатывается **браузером** (битая ссылка); бизнес-логика ElecLib при этом не прерывается.

---

## Слой Repository и Service (сводка)

| Функция | Repository / хранилище | Service / утилита |
|---------|------------------------|-------------------|
| Комикс (метаданные) | `BookRepository` → `books` | `BookService`, `AdminController` |
| PDF на диске | `ComicPdfStorageService` → `data/comics/` | `store()`, `hasPdf()`, `asResource()` |
| Прогресс PDF/текста | `ReadingPositionRepository` | `ReadingPositionService` |
| Поиск каталога | `BookRepository.searchByTitleOrAuthor()` | `BookService.searchByTitleOrAuthor()` |
| Оглавление | — (в памяти) | `ReaderToc`, `ReaderPaging` |
| Обложки в карточках | `BookRepository` (поле `cover_url`) | `BookService.toCardDtos()` |

Доступ к Supabase инкапсулирован в **`SupabaseClient`** (REST API); репозитории не используют JPA, а работают с JSON и таблицами PostgREST.

---

## Вывод о работоспособности программного модуля

В ходе выполнения задания реализованы и проверены программные модули, связанные с **тремя новыми функциями** ElecLib. Функции согласованы с пользовательским интерфейсом (задание 3) и алгоритмами (задание 2).

Проведённое тестирование (ручное) подтвердило:

- **корректность добавления комикса** администратором и чтения PDF пользователем с подпиской;
- **сохранение последней страницы** комикса и текстовой книги в `reading_position`;
- **работу поиска** по названию и автору с отображением полного каталога при пустом запросе;
- **построение оглавления** для текстов с заголовками и сообщение при их отсутствии;
- **отображение обложек** по URL и заглушек при пустом `coverUrl`;
- **обработку ошибок** (неверный PDF, отсутствие файла, доступ без подписки).

Реализованные модули встроены в архитектуру Spring Boot (Controller → Service → Repository / файловое хранилище) и обеспечивают стабильную работу веб-приложения.

---

## Диаграмма компонентов (по слоям)

На **рисунке 25** — многослойная диаграмма по образцу мобильного приложения (View → ViewModel → Model → база данных):

| Слой | ElecLib |
|------|---------|
| **Слой View** | `layout.html`, `books/list`, `book-card`, `read.html`, `read-comic.html`, `comics/list`, `admin/book-form` |
| **Controller / Service** (аналог ViewModel) | `BookController`, `AdminController`, `ComicController`, `BookService`, `ComicPdfStorageService`, `ReaderToc`, `ReadingPositionService` |
| **Слой Model** | `Book`, `BookCardDto`, `ReaderTocEntryDto`, `ReadingPosition` |
| **Слой данных** | `BookRepository`, `ReadingPositionRepository`, **Supabase** (цилиндр), **data/comics/** (PDF) |

Между слоями — **двунаправленные стрелки** (запрос страницы и ответ с данными).

Откройте [`figures/eleclib-component-diagram.drawio`](figures/eleclib-component-diagram.drawio) → вкладка **«Diagramma komponentov»** → экспорт PNG.

---

## Draw.io

| Файл | Рисунки |
|------|---------|
| [`eleclib-component-diagram.drawio`](figures/eleclib-component-diagram.drawio) | **25** — диаграмма компонентов (рекомендуется) |
| [`eleclib-assignment-4.drawio`](figures/eleclib-assignment-4.drawio) | 22–24 — логика функций; 25 — черновик |

Экспорт: **File → Export as → PNG**.
