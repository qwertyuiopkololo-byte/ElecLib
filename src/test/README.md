# Проверки новых функций ElecLib

Java 17, Maven, JUnit 5 и Mockito. Проверки изолированы от рабочей БД Supabase.

```powershell
.\mvnw.cmd test
```

Проверяются «Моя библиотека», прогресс на карточках, автоматические полки,
похожие книги, изменение профиля и пароля, обработка аватаров,
ежемесячные цели и защита чужих закладок/заметок.
`AvatarStorageServiceTest` проверяет HTTP-запросы через MockRestServiceServer.
Проверка повторного завершения подтверждает использование составного ключа
и режима игнорирования дубликатов, а не выполнение ограничений живой PostgreSQL.

## Проверки ридера в браузере

Нужны Node.js, Playwright и установленный Microsoft Edge:

```powershell
node --test src/test/browser/reader.test.cjs
```

Playwright должен быть доступен в Node.js. Если он установлен вне проекта,
задайте `RUNTIME_NODE_MODULES` — путь к каталогу `node_modules` с Playwright.

Тесты загружают настоящий HTML-шаблон ридера и его JavaScript в изолированной
странице. Проверяются поиск без учёта регистра, отсутствие совпадений,
переход к странице, ограничение 200 результатов, безопасность фрагментов
и взаимное закрытие панелей. Запуск сайта и авторизация не требуются.

Java-отчёты: `target/surefire-reports/`. Для XML-отчёта браузерных проверок:

```powershell
node --test --test-reporter=junit --test-reporter-destination=target/reader-ui-results.xml src/test/browser/reader.test.cjs
```
