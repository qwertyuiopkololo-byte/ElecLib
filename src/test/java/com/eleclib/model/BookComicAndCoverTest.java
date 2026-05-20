package com.eleclib.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookComicAndCoverTest {

    @Test
    void isComic_trueWhenContentTypeComic() {
        Book book = Book.builder().contentType(Book.CONTENT_TYPE_COMIC).build();
        assertTrue(book.isComic());
    }

    @Test
    void isComic_falseForTextOrNull() {
        assertFalse(Book.builder().contentType(Book.CONTENT_TYPE_TEXT).build().isComic());
        assertFalse(Book.builder().contentType(null).build().isComic());
    }

    @Test
    void coverUrl_storedOnBuilder() {
        Book book = Book.builder()
                .title("Тест")
                .coverUrl("https://example.com/cover.jpg")
                .build();
        assertEquals("https://example.com/cover.jpg", book.getCoverUrl());
    }
}
