package com.eleclib.util;

import com.eleclib.dto.ReaderTocEntryDto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Поиск заголовков оглавления в тексте книги (глава, §, римские цифры и т.д.)
 * и привязка к номеру страницы {@link ReaderPaging}.
 */
public final class ReaderToc {

    private static final int MAX_LINE_LEN = 150;
    private static final int MAX_TITLE_LEN = 120;
    private static final int MAX_ENTRIES = 250;

    private static final Pattern[] TOC_LINE_PATTERNS = {
            // Глава 1 / Глава IV. Название / Chapter 2
            Pattern.compile("(?iu)^(?:глава|chapter|часть|part|раздел|section|книга)\\s+(?:\\d+|[IVXLCDM]+)(?:\\s*[:.)\\-–—]?\\s*\\S.*)?$"),
            // § 1 / §12
            Pattern.compile("^§\\s*\\d+\\s*.*$"),
            // IV. Название / XII — ...
            Pattern.compile("^[IVXLCDM]{1,6}\\s*[:.)\\-–—]\\s+\\S+.*$"),
            // 1. Введение / 12 - Глава
            Pattern.compile("^\\d{1,3}\\s*[:.)\\-–—]\\s+[\\p{Lu}А-ЯЁ].*$"),
            // Markdown ###
            Pattern.compile("^#{1,3}\\s+\\S+.*$"),
            // Введение / Пролог / Эпилог
            Pattern.compile("(?iu)^(?:введение|пролог|эпилог|заключение|предисловие|послесловие)(?:\\s+.+|\\s*[:.)\\-–—]?\\s*)$"),
            // Отдельная строка «ГЛАВА» / «CHAPTER»
            Pattern.compile("(?iu)^(?:глава|chapter|часть|part|раздел|section)\\s*$"),
    };

    private ReaderToc() {
    }

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
            if (lineEnd < 0) {
                lineEnd = text.length();
            }
            String line = text.substring(i, lineEnd);
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.length() <= MAX_LINE_LEN && matchesTocLine(trimmed)) {
                String title = normalizeTitle(trimmed);
                if (!title.isEmpty()) {
                    int page = pageForCharOffset(pages, lineStart);
                    entries.add(ReaderTocEntryDto.builder().title(title).pageNumber(page).build());
                }
            }
            lineStart = lineEnd < text.length() ? lineEnd + 1 : lineEnd;
            i = lineEnd < text.length() ? lineEnd + 1 : lineEnd;
        }
        return dedupe(entries);
    }

    static boolean matchesTocLine(String trimmedLine) {
        for (Pattern p : TOC_LINE_PATTERNS) {
            if (p.matcher(trimmedLine).matches()) {
                return true;
            }
        }
        return false;
    }

    static int pageForCharOffset(List<String> pages, int charOffset) {
        if (charOffset < 0) {
            return 1;
        }
        int pos = 0;
        for (int i = 0; i < pages.size(); i++) {
            pos += pages.get(i).length();
            if (charOffset < pos) {
                return i + 1;
            }
        }
        return Math.max(1, pages.size());
    }

    private static String normalizeTitle(String line) {
        String t = line.replaceAll("\\s+", " ").trim();
        if (t.length() > MAX_TITLE_LEN) {
            t = t.substring(0, MAX_TITLE_LEN) + "…";
        }
        return t;
    }

    private static List<ReaderTocEntryDto> dedupe(List<ReaderTocEntryDto> entries) {
        List<ReaderTocEntryDto> out = new ArrayList<>();
        String lastKey = null;
        for (ReaderTocEntryDto e : entries) {
            String key = e.getPageNumber() + "|" + e.getTitle();
            if (!key.equals(lastKey)) {
                out.add(e);
                lastKey = key;
            }
        }
        return out;
    }
}
