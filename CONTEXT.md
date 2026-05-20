# CONTEXT.md — ElecLib (handoff Cursor → Antigravity)

## Обновление 31.10.2026: редизайн Hallmark

- Выполнен редизайн Reading Room: бумажный фон, зелёные действия, Georgia для заголовков, адаптивный каталог и формы.
- Система оформления: `design.md`; общие файлы: `src/main/resources/static/css/{style,tokens,base}.css`.
- Шаблоны используют прежние маршруты, поля и JS. Проверены 27 шаблонов, 663 привязки и 8 скриптов; существующие 31 тест проходят.
- Исходные незакоммиченные UI-изменения сохранены в `.hallmark/original` и `.hallmark/pre-redesign-backup.zip`. Самостоятельно не коммитились и не сбрасывались.
- Локальное сравнение: `.hallmark/compare.html`, скриншоты: `.hallmark/screens`, отчёт: `.hallmark/report.md`. Папка `.hallmark` исключена из Git.
- Supabase при предыдущем запуске недоступен. Проверка каталога/карточки/жанров выполнена на локальных read-only fixtures из `supabase_sample_data.sql`; персональные страницы визуально со своими данными пока не проверены.
- Временные процессы предпросмотра: 8091 — исходный backend с новыми ресурсами; 8092 — новые ресурсы с локальными примерами (fixture server 18093). Это не постоянная конфигурация приложения.
- Остальные новые функции курсовой ещё предстоит реализовать; в согласованный список также входят ежемесячные цели чтения.

> Снимок состояния на конец сессии Cursor. Читать первым перед любой новой работой.

---

## 1) Статус и архитектура

**Проект:** веб-библиотека электронных книг **ElecLib** (учебная / курсовая + практика).  
**Стек:** Java 17 · Spring Boot · Spring Security (BCrypt, роли `app_user` / `admin`) · Thymeleaf · PostgreSQL через **Supabase PostgREST** (не JDBC).  
**Архитектура:** MVC — `Controller` → `Service` → `Repository` → `SupabaseClient` (JSON REST). Модели/DTO без JPA.  
**Схема БД:** `supabase_schema.sql` (применять в Supabase SQL Editor).  
**Конфиг:** `supabase.url` / `supabase.key` из env или `application-local.properties` (секреты не коммитить). Приложение ходит в Supabase с **service key** в `Authorization`, не с JWT пользователя.  
**Запуск:** `./mvnw spring-boot:run` или IDE; локально обычно `http://localhost:8080`.

**Доменные зоны:**
| Зона | Ключевые классы / маршруты |
|------|----------------------------|
| Auth | `AuthController`, `UserService`, `SecurityConfig` |
| Каталог | `BookController` `/books`, `GenreController`, поиск `q`, фильтр жанра |
| Подписка | `SubscriptionController` — имитация оплаты (1 мес. active) |
| Ридер | `/books/{id}/read` — страницы (`ReaderPaging` 2500 симв.), прогресс, закладки, заметки, TOC (`ReaderToc`), тема/шрифт в `localStorage` |
| Комиксы | `ComicController`, `ComicPdfStorageService`, `content_type=comic` |
| Избранное / отзывы / рекомендации | `Favorite*`, `BookRating*`, `BookService.getRecommendedBooks` |
| Админ | `/admin/**` — книги, авторы, жанры, пользователи, подписки |
| **Уже добавленные «новые» фичи** | Статистика `/reading/stats`, полки `/shelves`, баннер «Продолжить чтение» на `/books` |

**Данные полок:** `reading_shelf` + `reading_shelf_book`; системные ключи `want` / `reading` / `done` (взаимоисключение при add на системную полку). Создание полки: **POST `/shelves/create`** (не `/shelves/new`).

---

## 2) Что уже сделано

### Функционал (код)
- Базовый цикл: регистрация/вход, каталог, карточка, подписка-гейт на чтение, ридер, закладки/заметки, избранное, оценки 1–5, рекомендации, админка.
- Комиксы (PDF), оглавление текстовых книг, настройки ридера (UI).
- **Статистика чтения** — `ReadingStatsController` / `ReadingStatsService` (агрегаты + недельная активность по `updated_at`).
- **Полки** — `ReadingShelfController` / `ReadingShelfService` (системные + custom, add/remove с карточки и со страницы полки).
- **Continue reading** — `ContinueReadingService` → атрибут `continueReading` в `BookController.list`.
- `LenientInstantDeserializer` в ObjectMapper Supabase — фикс нулей в недельной статистике из‑за дат PostgREST.
- Расширения репозиториев: `ReadingPositionRepository.findByUserId`, `findByUserId` у закладок/заметок, `BookService.findCardsByBookIdsInOrder`.
- Рефакторинг Inspect Code: **740 → 222** warnings (−70,27%); HTML/Thymeleaf `lang=ru`, label/id; константы `TABLE` в репозиториях; укорочен `BookController.read()`; мелкие фиксы `FavoriteController` / `AuthController` / `ReadingPositionService`.

### Документация / практика
- Практика: `docs/practice/assignment-2.md` … `assignment-8.md`, `conclusion.md`, `code-listings-new-features.md`, фигуры в `docs/practice/figures/`.
- Сводная таблица рефакторинга: `docs/Таблица_рефакторинг_Inspect_Code_ElecLib.txt`.
- В чате готовились ТЗ/руководство пользователя/листинги с метками `[ДОБАВЛЕНО]` — файлы могли быть переименованы/перенесены; искать в `docs/` и `docs/practice/`.

### Согласованный план **6 функций для курсовой** (ещё **не реализованы** в коде)
1. **Моя библиотека** `/library` — агрегация прогресса, избранного, полок, последних закладок/заметок.  
2. **Прогресс на карточках** — стр. X из Y / % в `BookCardDto` + шаблоны.  
3. **Авто-полки** — при сохранении позиции → «Читаю»; последняя страница → «Прочитано».  
4. **Похожие книги** на карточке (автор/жанр; исключить текущую/избранное).  
5. **Поиск внутри книги** в ридере (подстрока → страница через `ReaderPaging`).  
6. **Профиль** — редактирование имени/фамилии, смена пароля (BCrypt).  

*(Ранее обсуждавшиеся цели чтения / экспорт / баннер подписки для п.4–6 отменены.)*

---

## 3) Нерешённые проблемы / долг

| Приоритет | Проблема |
|-----------|----------|
| High | **6 курсовых фич** (п.2 выше) — кода нет; нет FR/AT в репозитории под финальный набор 4–6. |
| Med | Inspect Code: остаток ~222 warnings; высокая сложность `ReaderToc.build()` / `toCardDtos()`; секреты в local properties. |
| Med | `@SneakyThrows` в `SupabaseClient` — полный отказ не сделан. |
| Med | Каталог/поиск часто **фильтруют в памяти** после `findAll` — масштабирование через PostgREST `ilike` не внедрено. |
| Med | Настройки ридера только в **localStorage** — нет серверного профиля. |
| Low | RLS в Supabase формально есть, но доступ через service key = фактически полный доступ с бэка. |
| Low | Вне scope ТЗ: реальная оплата, DRM, мобильное/офлайн, соцсеть, CRM. |
| Note | Часть русских docs из чата (ТЗ, руководство, полный листинг) может отсутствовать в текущем дереве — при необходимости восстановить из истории/пересоздать. |

---

## 4) Пошаговый план для следующего агента

1. **Проверить окружение:** `supabase_schema.sql` применён (в т.ч. `reading_shelf*`); заданы `SUPABASE_URL` / `SUPABASE_KEY`; `./mvnw test` зелёный.  
2. **Не ломать** существующие полки/статистику/баннер; маршруты полок: `POST /shelves/create`.  
3. **Реализовать курсовые фичи по порядку 1→6** (зависимости: 3 опирается на `ReadingShelfService` + `ReadingPositionService`; 2 — на `ReaderPaging` + позиции; 4 — на `BookService`; 5 — ридер; 6 — `UserService` + Security).  
   - Для каждой: схема (если нужна) → model/repo → service → controller → Thymeleaf → пункт меню → 1–2 AT/unit там, где логика чистая.  
4. **Документы курсовой:** обновить ТЗ (границы, FR, AT), руководство пользователя, таблицу тестов; листинги — только ключевые методы новых фич.  
5. **Качество (опционально после фич):** убрать `@SneakyThrows` / сузить исключения; декомпозировать `ReaderToc.build`; server-side search; не коммитить ключи.  
6. **Не делать** без явного запроса: мобильное приложение, Stripe, DRM, переписывание на JPA.

**Быстрые ориентиры в коде:**  
`BookController`, `BookService`, `ReadingShelfService`, `ReadingStatsService`, `ContinueReadingService`, `ReaderPaging`, `ReaderToc`, `SupabaseClient`, `SecurityConfig`, `supabase_schema.sql`.

**Язык артефактов для отчёта:** русский. Код/коммиты — в стиле существующего репозитория (английские имена классов, краткие сообщения).
