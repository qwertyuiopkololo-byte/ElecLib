package com.eleclib.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Должен совпадать с логикой разбиения на страницы в {@code books/read.html} (CHARS_PER_PAGE).
 */
public final class ReaderPaging {

    public static final int CHARS_PER_PAGE = 2500;

    private ReaderPaging() {
    }

    public static int totalPages(String text) {
        return Math.max(1, splitIntoPages(text).size());
    }

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
                int lastSpace = chunk.lastIndexOf(' ');
                int lastNewline = chunk.lastIndexOf('\n');
                int breakAt = Math.max(lastSpace, lastNewline);
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
}
